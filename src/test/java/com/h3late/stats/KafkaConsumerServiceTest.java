package com.h3late.stats;

import com.h3late.stats.dto.VideoEventDto;
import com.h3late.stats.service.KafkaConsumerService;
import com.h3late.stats.service.LivestreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class KafkaConsumerServiceTest {

    private LivestreamService livestreamService;
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    public void setUp() {
        livestreamService = Mockito.mock(LivestreamService.class);
        kafkaConsumerService = new KafkaConsumerService(livestreamService);
    }

    @Test
    public void listenToVideoEvents_withValidMessage_thenDelegatesToLivestreamService() {
        // Arrange
        String videoId = "test-video-001";
        VideoEventDto messageBody = new VideoEventDto("Test Title");

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, messageBody);

        // Assert
        verify(livestreamService, times(1)).processLivestreamEvent(videoId, messageBody);
    }

    @Test
    public void listenToVideoEvents_withNullMessage_thenDelegatesToLivestreamService() {
        // Arrange
        String videoId = "test-video-002";

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, null);

        // Assert
        verify(livestreamService, times(1)).processLivestreamEvent(videoId, null);
    }

    @Test
    public void listenToVideoEvents_withDifferentVideoIds_thenDelegatesCorrectly() {
        // Arrange
        String videoId1 = "video-aaa";
        String videoId2 = "video-bbb";
        VideoEventDto messageBody1 = new VideoEventDto("First Stream");
        VideoEventDto messageBody2 = new VideoEventDto("Second Stream");

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId1, messageBody1);
        kafkaConsumerService.listenToVideoEvents(videoId2, messageBody2);

        // Assert
        verify(livestreamService, times(1)).processLivestreamEvent(videoId1, messageBody1);
        verify(livestreamService, times(1)).processLivestreamEvent(videoId2, messageBody2);
        verify(livestreamService, times(2)).processLivestreamEvent(anyString(), any());
    }

    @Test
    public void listenToVideoEvents_withEmptyTitle_thenDelegatesToLivestreamService() {
        // Arrange
        String videoId = "test-video-003";
        VideoEventDto messageBody = new VideoEventDto("");

        // Act
        kafkaConsumerService.listenToVideoEvents(videoId, messageBody);

        // Assert
        verify(livestreamService, times(1)).processLivestreamEvent(videoId, messageBody);
    }
}
