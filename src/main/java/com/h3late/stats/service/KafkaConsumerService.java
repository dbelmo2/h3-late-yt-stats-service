package com.h3late.stats.service;

import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.repository.LivestreamRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final YoutubeApiService youtubeApiService;
    private final LivestreamRepository livestreamRepository;

    @KafkaListener(topics = "${kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenToVideoEvents(
            @Header(KafkaHeaders.RECEIVED_KEY)
            String videoId,
            @Payload(required = false)
            VideoEventDto messageBody
    ) {
        log.info("Received kafka message with key '{}' and body=[{}]", videoId, messageBody);

        if (messageBody == null) {
            processCancelledLivestream(videoId);
        } else {
            youtubeApiService.getVideoDetails(videoId)
                    .ifPresent((videoItem) -> {

                        log.info("Fetched livestream details for '{}' with videoId=[{}]", videoItem.getSnippet().getTitle(), videoId);
                        Optional<Livestream> existingVideo = livestreamRepository.findById(videoId);

                        // Skip if not a livestream
                        if (videoItem.getStatus().getUploadStatus().equals("processed")) {
                            log.info("Livestream '{}' with videoId=[{}] has status 'processed'. This is likely not a livestream", videoItem.getSnippet().getTitle(), videoId);
                            return;
                        }

                        if (existingVideo.isPresent()) {
                            // Update if livestream already exists
                            updateExistingLivestream(
                                    videoId,
                                    messageBody.getTitle(),
                                    videoItem,
                                    existingVideo.get()
                            );
                        } else {
                            // Create new livestream if it doesn't exist
                            Livestream newStream = createNewLivestream(videoId, videoItem);
                            livestreamRepository.save(newStream);
                        }
                    });
        }
    }

    public void updateExistingLivestream(
            String videoId,
            String title,
            YoutubeApiResponseDto.VideoItem videoItem,
            Livestream existingVideo
    ) {
        log.info("Livestream with videoId=[{}] already exists in the database.", videoId);
        boolean hasUpdates = false;

        if (!title.equals(existingVideo.getTitle())) {
            log.info("New title '{}' is different from existing title '{}' for videoId=[{}]", title, existingVideo.getTitle(), videoId);
            existingVideo.setTitle(title);
            hasUpdates = true;
        }

        YoutubeApiResponseDto.LiveStreamingDetails liveDetails = videoItem.getLiveStreamingDetails();

        if (liveDetails == null) {
            log.warn("Live streaming details are missing for videoId=[{}]. Skipping livestream processing.", videoId);
        }

        if (
                existingVideo.getActualStart() == null
                && liveDetails != null
                && liveDetails.getActualStartTime() != null
        ) {
            existingVideo.setStatus((StreamStatus.LIVE));
            Instant actualStart = Instant.parse(liveDetails.getActualStartTime());
            existingVideo.setActualStart(actualStart);
            long lateTimeSeconds = java.time.Duration.between(
                    existingVideo.getScheduledStart(),
                    actualStart
            ).getSeconds();
            if (lateTimeSeconds > 10) {
                existingVideo.setTimeStatus(TimeStatus.LATE);
            } else if (lateTimeSeconds < 0) {
                existingVideo.setTimeStatus(TimeStatus.EARLY);
            } else {
                existingVideo.setTimeStatus(TimeStatus.ON_TIME);
            }
            existingVideo.setDiffSeconds(lateTimeSeconds);
            log.info("Livestream with videoId=[{}] has started. Actual start time: {}, Scheduled start time: {}, Late time (seconds): {}, Time status: {}",
                    videoId, actualStart, existingVideo.getScheduledStart(), lateTimeSeconds, existingVideo.getTimeStatus()
            );
            hasUpdates = true;
        }

        if (
                existingVideo.getActualEnd() == null
                && liveDetails != null
                && liveDetails.getActualEndTime() != null
        ) {
            existingVideo.setStatus(StreamStatus.ENDED);
            Instant actualEnd = Instant.parse(liveDetails.getActualEndTime());
            long totalDurationSeconds = java.time.Duration.between(
                    existingVideo.getActualStart(),
                    actualEnd
            ).getSeconds();
            existingVideo.setActualEnd(actualEnd);
            existingVideo.setTotalDurationSeconds(totalDurationSeconds);
            log.info("Livestream with videoId=[{}] has ended with a total duration of {}. Actual end time: {}", videoId, totalDurationSeconds, actualEnd);
            hasUpdates = true;
        }


        if (!hasUpdates) {
            log.info("No relevant updates for '{}' with videoId=[{}] Skipping processing", title, videoId);
        } else {
            livestreamRepository.save(existingVideo);
            log.info("Updated livestream with videoId=[{}]", videoId);
        }
    }

    public Livestream createNewLivestream(String videoId, YoutubeApiResponseDto.VideoItem videoItem) {
        Livestream newLivestream = new Livestream();
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails = videoItem.getLiveStreamingDetails();
        if (liveDetails != null && liveDetails.getScheduledStartTime() != null) {
            newLivestream.setScheduledStart(Instant.parse(liveDetails.getScheduledStartTime()));
        }
        newLivestream.setVideoId(videoId);
        newLivestream.setTitle(videoItem.getSnippet().getTitle());
        newLivestream.setStatus(StreamStatus.SCHEDULED);

        if (liveDetails != null && liveDetails.getActualStartTime() != null) {
            updateExistingLivestream(
                    videoId,
                    videoItem.getSnippet().getTitle(),
                    videoItem,
                    newLivestream
            );
        }
        return newLivestream;
    }


    public void processCancelledLivestream(String videoId) {
        log.info("Received null message body for videoId=[{}]. Marking livestream as cancelled if it exists.", videoId);
        Optional<Livestream> existingVideo = livestreamRepository.findById(videoId);
        if (existingVideo.isPresent()) {
            Livestream livestream = existingVideo.get();
            livestream.setStatus(StreamStatus.CANCELLED);
            livestream.setTimeStatus(TimeStatus.CANCELLED);
            livestreamRepository.save(livestream);
            log.info("Marked livestream with videoId=[{}] as cancelled.", videoId);
        } else {
            log.info("No existing livestream found for videoId=[{}] to mark as cancelled.", videoId);
        }
    }
}
