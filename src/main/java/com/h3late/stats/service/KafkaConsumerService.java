package com.h3late.stats.service;

import com.h3late.stats.dto.VideoEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final LivestreamService livestreamService;

    @KafkaListener(topics = "${kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenToVideoEvents(
            @Header(KafkaHeaders.RECEIVED_KEY)
            String videoId,
            @Payload(required = false)
            VideoEventDto messageBody
    ) {
        log.info("Received kafka message with key '{}' and body=[{}]", videoId, messageBody);

        livestreamService.processLivestreamEvent(videoId, messageBody);
    }
}
