package com.h3late.stats.service;

import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.repository.LivestreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
    public void listenToVideoEvents(VideoEventDto messageBody) {
        final String videoId = messageBody.getVideoId();
        log.info("Received message=[{}]", messageBody);

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

    public void updateExistingLivestream(
            String videoId,
            YoutubeApiResponseDto.VideoItem videoItem,
            Livestream existingVideo
    ) {
        log.info("Livestream with videoId=[{}] already exists in the database.", videoId);
        boolean hasUpdates = false;
        final String newTitle = videoItem.getSnippet().getTitle();

        if (!newTitle.equals(existingVideo.getTitle())) {
            log.info("New title '{}' is different from existing title '{}' for videoId=[{}]", newTitle, existingVideo.getTitle(), videoId);
            existingVideo.setTitle(newTitle);
            hasUpdates = true;
        }

        if (
                existingVideo.getActualStart() == null
                && videoItem.getLiveStreamingDetails().getActualStartTime() != null
        ) {
            existingVideo.setStatus((StreamStatus.LIVE));
            Instant actualStart = Instant.parse(videoItem.getLiveStreamingDetails().getActualStartTime());
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
            log.info("Livestream with videoId=[{}] has started. Actual start time: {}, Scheduled start time: {}, Late time (seconds): {}, Time status: {}",
                    videoId, actualStart, existingVideo.getScheduledStart(), lateTimeSeconds, existingVideo.getTimeStatus()
            );
            hasUpdates = true;
        }

        if (
                existingVideo.getActualEnd() == null
                && videoItem.getLiveStreamingDetails().getActualEndTime() != null
        ) {
            existingVideo.setStatus(StreamStatus.ENDED);
            Instant actualEnd = Instant.parse(videoItem.getLiveStreamingDetails().getActualEndTime());
            long totalDurationSeconds = java.time.Duration.between(
                    existingVideo.getActualStart(),
                    actualEnd
            ).getSeconds();
            existingVideo.setActualEnd(actualEnd);
            existingVideo.setTotalDurationSeconds(totalDurationSeconds);
            log.info("Livestream with videoId=[{}] has ended. Actual end time: {}", videoId, actualEnd);
            hasUpdates = true;
        }


        if (!hasUpdates) {
            log.info("No relevant updates for '{}' with videoId=[{}] Skipping processing", newTitle, videoId);
        } else {
            livestreamRepository.save(existingVideo);
            log.info("Updated livestream with videoId=[{}]", videoId);
        }
    }

    public Livestream createNewLivestream(String videoId, YoutubeApiResponseDto.VideoItem videoItem) {
        Livestream newLivestream = new Livestream();
        Instant scheduledStart = Instant.parse(videoItem.getLiveStreamingDetails().getScheduledStartTime());
        newLivestream.setVideoId(videoId);
        newLivestream.setTitle(videoItem.getSnippet().getTitle());
        newLivestream.setStatus(StreamStatus.SCHEDULED);
        newLivestream.setScheduledStart(scheduledStart);
        return newLivestream;
    }

}
