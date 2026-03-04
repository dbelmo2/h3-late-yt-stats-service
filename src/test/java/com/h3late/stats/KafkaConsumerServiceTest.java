package com.h3late.stats;

import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.repository.LivestreamRepository;
import com.h3late.stats.service.KafkaConsumerService;
import com.h3late.stats.service.YoutubeApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class KafkaConsumerServiceTest {

    private YoutubeApiService youtubeApiService;
    private LivestreamRepository livestreamRepository;
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    public void setUp() {
        // Create fresh mocks for each test
        youtubeApiService = Mockito.mock(YoutubeApiService.class);
        livestreamRepository = Mockito.mock(LivestreamRepository.class);
        kafkaConsumerService = new KafkaConsumerService(youtubeApiService, livestreamRepository);
    }

    // ==================== HELPER METHODS ====================

    private YoutubeApiResponseDto.VideoItem createVideoItem(
            String title,
            String uploadStatus,
            String scheduledStartTime,
            String actualStartTime,
            String actualEndTime
    ) {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet(title, "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status(uploadStatus);
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails =
                new YoutubeApiResponseDto.LiveStreamingDetails(actualStartTime, scheduledStartTime, actualEndTime);
        return new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);
    }

    private Livestream createExistingLivestream(
            String videoId,
            String title,
            Instant scheduledStart,
            Instant actualStart,
            Instant actualEnd,
            StreamStatus status,
            TimeStatus timeStatus,
            Long diffSeconds,
            Long totalDurationSeconds
    ) {
        Livestream livestream = new Livestream();
        livestream.setVideoId(videoId);
        livestream.setTitle(title);
        livestream.setScheduledStart(scheduledStart);
        livestream.setActualStart(actualStart);
        livestream.setActualEnd(actualEnd);
        livestream.setStatus(status);
        livestream.setTimeStatus(timeStatus);
        livestream.setDiffSeconds(diffSeconds);
        livestream.setTotalDurationSeconds(totalDurationSeconds);
        return livestream;
    }

    // ==================== CREATE NEW LIVESTREAM TESTS ====================

    @Test
    public void whenNewLivestreamReceived_thenCreateWithScheduledStatus() {
        // Arrange
        String videoId = "new-video-001";
        String title = "My New Livestream";
        String scheduledStart = "2026-03-04T10:00:00Z";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(videoId, saved.getVideoId());
        assertEquals(title, saved.getTitle());
        assertEquals(StreamStatus.SCHEDULED, saved.getStatus());
        assertEquals(Instant.parse(scheduledStart), saved.getScheduledStart());
        assertNull(saved.getActualStart());
        assertNull(saved.getActualEnd());
    }

    @Test
    public void whenNewLivestreamWithoutScheduledStart_thenCreateWithNullScheduledStart() {
        // Arrange
        String videoId = "new-video-002";
        String title = "Livestream No Schedule";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", null, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(videoId, saved.getVideoId());
        assertEquals(StreamStatus.SCHEDULED, saved.getStatus());
        assertNull(saved.getScheduledStart());
    }

    // ==================== TITLE UPDATE TESTS ====================

    @Test
    public void whenTitleChanges_thenUpdateTitle() {
        // Arrange
        String videoId = "title-update-001";
        String oldTitle = "Old Title";
        String newTitle = "New Title";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, oldTitle, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                newTitle, "succeeded", scheduledStart.toString(), null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(newTitle));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(newTitle, saved.getTitle());
    }

    @Test
    public void whenTitleUnchanged_thenNoSave() {
        // Arrange
        String videoId = "title-unchanged-001";
        String title = "Same Title";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== ACTUAL START TIME - ON TIME ====================

    @Test
    public void whenActualStartWithin10Seconds_thenStatusOnTime() {
        // Arrange
        String videoId = "ontime-001";
        String title = "On Time Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z"); // 5 seconds late

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(actualStart, saved.getActualStart());
        assertEquals(5L, saved.getDiffSeconds());
    }

    @Test
    public void whenActualStartExactly10Seconds_thenStatusOnTime() {
        // Arrange - Testing the boundary (10 seconds should be ON_TIME)
        String videoId = "ontime-exact-001";
        String title = "Exact 10 Seconds Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:10Z"); // Exactly 10 seconds

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(10L, saved.getDiffSeconds());
    }

    // ==================== ACTUAL START TIME - LATE ====================

    @Test
    public void whenActualStartMoreThan10Seconds_thenStatusLate() {
        // Arrange
        String videoId = "late-001";
        String title = "Late Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:30Z"); // 30 seconds late

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.LATE, saved.getTimeStatus());
        assertEquals(30L, saved.getDiffSeconds());
    }

    @Test
    public void whenActualStart11SecondsLate_thenStatusLate() {
        // Arrange - Testing the boundary (11 seconds should be LATE)
        String videoId = "late-boundary-001";
        String title = "Boundary Late Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:11Z"); // 11 seconds late

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.LATE, saved.getTimeStatus());
        assertEquals(11L, saved.getDiffSeconds());
    }

    // ==================== ACTUAL START TIME - EARLY ====================

    @Test
    public void whenActualStartBeforeScheduled_thenStatusEarly() {
        // Arrange
        String videoId = "early-001";
        String title = "Early Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T09:59:45Z"); // 15 seconds early

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.EARLY, saved.getTimeStatus());
        assertEquals(-15L, saved.getDiffSeconds());
    }

    // ==================== ACTUAL START TIME - ALREADY SET ====================

    @Test
    public void whenActualStartAlreadySet_thenNoUpdate() {
        // Arrange
        String videoId = "already-started-001";
        String title = "Already Started Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant existingActualStart = Instant.parse("2026-03-04T10:00:05Z");
        Instant newActualStart = Instant.parse("2026-03-04T10:00:30Z"); // Different start time from API

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, existingActualStart, null,
                StreamStatus.LIVE, TimeStatus.ON_TIME, 5L, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), newActualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert - Should not save since actualStart is already set and title is same
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== ACTUAL END TIME TESTS ====================

    @Test
    public void whenActualEndTimeReceived_thenStatusEnded() {
        // Arrange
        String videoId = "ended-001";
        String title = "Ended Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z");
        Instant actualEnd = Instant.parse("2026-03-04T12:00:05Z"); // 2 hours duration

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, actualStart, null,
                StreamStatus.LIVE, TimeStatus.ON_TIME, 5L, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), actualEnd.toString()
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.ENDED, saved.getStatus());
        assertEquals(actualEnd, saved.getActualEnd());
        assertEquals(7200L, saved.getTotalDurationSeconds()); // 2 hours = 7200 seconds
    }

    @Test
    public void whenActualEndAlreadySet_thenNoUpdate() {
        // Arrange
        String videoId = "already-ended-001";
        String title = "Already Ended Stream";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z");
        Instant existingActualEnd = Instant.parse("2026-03-04T11:00:05Z");
        Instant newActualEnd = Instant.parse("2026-03-04T12:00:05Z"); // Different end time

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, actualStart, existingActualEnd,
                StreamStatus.ENDED, TimeStatus.ON_TIME, 5L, 3600L
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "succeeded", scheduledStart.toString(), actualStart.toString(), newActualEnd.toString()
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert - Should not save since actualEnd is already set and title is same
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== CANCELLED LIVESTREAM TESTS ====================

    @Test
    public void whenNullBodyAndLivestreamExists_thenMarkCancelled() {
        // Arrange
        String videoId = "cancelled-001";
        String title = "To Be Cancelled";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, null);

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());
        verify(youtubeApiService, never()).getVideoDetails(anyString());

        Livestream saved = captor.getValue();
        assertEquals(StreamStatus.CANCELLED, saved.getStatus());
        assertEquals(TimeStatus.CANCELLED, saved.getTimeStatus());
    }

    @Test
    public void whenNullBodyAndLivestreamDoesNotExist_thenNoSave() {
        // Arrange
        String videoId = "nonexistent-001";

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, null);

        // Assert
        verify(livestreamRepository, never()).save(any());
        verify(youtubeApiService, never()).getVideoDetails(anyString());
    }

    // ==================== PROCESSED STATUS (NON-LIVESTREAM) TESTS ====================

    @Test
    public void whenUploadStatusIsProcessed_thenSkipProcessing() {
        // Arrange
        String videoId = "processed-001";
        String title = "Not A Livestream";

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                title, "processed", null, null, null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== API ERROR HANDLING TESTS ====================

    @Test
    public void whenYoutubeApiReturnsEmpty_thenNoSave() {
        // Arrange
        String videoId = "api-error-001";
        String title = "API Error Stream";

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.empty());
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.empty());

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== NULL LIVE STREAMING DETAILS TESTS ====================

    @Test
    public void whenLiveStreamingDetailsIsNull_thenNoUpdateToExisting() {
        // Arrange
        String videoId = "null-details-001";
        String title = "Stream With Null Details";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");

        Livestream existing = createExistingLivestream(
                videoId, title, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet(title, "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status("succeeded");
        YoutubeApiResponseDto.VideoItem videoItem = new YoutubeApiResponseDto.VideoItem(snippet, status, null);

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(title));

        // Assert - No save because title is same and no live details to update
        verify(livestreamRepository, never()).save(any());
    }

    // ==================== COMBINED UPDATE TESTS ====================

    @Test
    public void whenTitleChangesAndStreamStarts_thenBothUpdated() {
        // Arrange
        String videoId = "combined-001";
        String oldTitle = "Old Title";
        String newTitle = "New Title";
        Instant scheduledStart = Instant.parse("2026-03-04T10:00:00Z");
        Instant actualStart = Instant.parse("2026-03-04T10:00:05Z");

        Livestream existing = createExistingLivestream(
                videoId, oldTitle, scheduledStart, null, null,
                StreamStatus.SCHEDULED, null, null, null
        );

        YoutubeApiResponseDto.VideoItem videoItem = createVideoItem(
                newTitle, "succeeded", scheduledStart.toString(), actualStart.toString(), null
        );

        when(livestreamRepository.findById(videoId)).thenReturn(Optional.of(existing));
        when(youtubeApiService.getVideoDetails(videoId)).thenReturn(Optional.of(videoItem));

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, new VideoEventDto(newTitle));

        // Assert
        ArgumentCaptor<Livestream> captor = ArgumentCaptor.forClass(Livestream.class);
        verify(livestreamRepository, times(1)).save(captor.capture());

        Livestream saved = captor.getValue();
        assertEquals(newTitle, saved.getTitle());
        assertEquals(StreamStatus.LIVE, saved.getStatus());
        assertEquals(TimeStatus.ON_TIME, saved.getTimeStatus());
        assertEquals(actualStart, saved.getActualStart());
    }
}
