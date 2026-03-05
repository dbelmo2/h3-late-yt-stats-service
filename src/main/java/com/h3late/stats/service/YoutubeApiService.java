package com.h3late.stats.service;

import com.h3late.stats.dto.YoutubeApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Service
public class YoutubeApiService {

    public final String apiKey;
    public final RestClient restClient;
    public final String baseUrl = "https://www.googleapis.com/youtube/v3";

    public YoutubeApiService(
            @Value("${youtube.api-key}")
            String apiKey
    ) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    public Optional<YoutubeApiResponseDto.VideoItem> getVideoDetails(String videoId) {
        log.info("Fetching video details for videoId=[{}]", videoId);
        try {
            YoutubeApiResponseDto response = this.restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("part", "snippet,status,liveStreamingDetails")
                            .queryParam("id", videoId)
                            .queryParam("key", this.apiKey)
                            .build())
                    .retrieve()
                    .body(YoutubeApiResponseDto.class);
            if (response != null && !response.getItems().isEmpty()) {
                return Optional.of(response.getItems().getFirst());
            }
        } catch (Exception e) {
            log.error("Failed to fetch video details for videoId: {}", videoId, e);
            return Optional.empty();
        }
        return Optional.empty();
    }

}
