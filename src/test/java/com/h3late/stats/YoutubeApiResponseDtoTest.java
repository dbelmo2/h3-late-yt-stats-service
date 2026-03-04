package com.h3late.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h3late.stats.dto.YoutubeApiResponseDto;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YoutubeApiResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testYoutubeApiResponseDtoCreation() {
        YoutubeApiResponseDto dto = new YoutubeApiResponseDto();
        assertNotNull(dto);
    }

    @Test
    public void testVideoItemCreation() {
        YoutubeApiResponseDto.VideoItem item = new YoutubeApiResponseDto.VideoItem();
        assertNotNull(item);
    }

    @Test
    public void testSnippetCreation() {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet();
        assertNotNull(snippet);
    }

    @Test
    public void testStatusCreation() {
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status();
        assertNotNull(status);
    }

    @Test
    public void testLiveStreamingDetailsCreation() {
        YoutubeApiResponseDto.LiveStreamingDetails details = new YoutubeApiResponseDto.LiveStreamingDetails();
        assertNotNull(details);
    }

    @Test
    public void testCompleteVideoItemMapping() {
        YoutubeApiResponseDto.VideoItem item = getVideoItem();

        assertEquals("Test Video Title", item.getSnippet().getTitle());
        assertEquals("live", item.getSnippet().getLiveBroadcastContent());
        assertEquals("succeeded", item.getStatus().getUploadStatus());
        assertEquals("2026-03-04T10:00:00Z", item.getLiveStreamingDetails().getActualStartTime());
        assertEquals("2026-03-04T12:00:00Z", item.getLiveStreamingDetails().getActualEndTime());
    }

    private static YoutubeApiResponseDto.@NonNull VideoItem getVideoItem() {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet("Test Video Title", "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status("succeeded");
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails =
                new YoutubeApiResponseDto.LiveStreamingDetails("2026-03-04T10:00:00Z", "2026-03-04T11:00:00Z", "2026-03-04T12:00:00Z");

        return new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);
    }

    @Test
    public void testYoutubeApiResponseDeserialization() throws Exception {
        String json = """
                {
                  "items": [
                    {
                      "snippet": {
                        "title": "Live Stream Title",
                        "liveBroadcastContent": "live"
                      },
                      "status": {
                        "uploadStatus": "succeeded"
                      },
                      "liveStreamingDetails": {
                        "actualStartTime": "2026-03-04T10:00:00Z",
                        "actualEndTime": "2026-03-04T12:00:00Z",
                        "scheduledStartTime": "2026-03-04T09:00:00Z"
                      }
                    }
                  ]
                }""";

        YoutubeApiResponseDto response = objectMapper.readValue(json, YoutubeApiResponseDto.class);

        assertNotNull(response);
        assertNotNull(response.getItems());
        assertEquals(1, response.getItems().size());

        YoutubeApiResponseDto.VideoItem item = response.getItems().getFirst();
        assertEquals("Live Stream Title", item.getSnippet().getTitle());
        assertEquals("live", item.getSnippet().getLiveBroadcastContent());
        assertEquals("succeeded", item.getStatus().getUploadStatus());
        assertEquals("2026-03-04T10:00:00Z", item.getLiveStreamingDetails().getActualStartTime());
        assertEquals("2026-03-04T12:00:00Z", item.getLiveStreamingDetails().getActualEndTime());
    }

    @Test
    public void testYoutubeApiResponseDeserializationWithExtraFields() throws Exception {
        String json = """
                {
                  "kind": "youtube#videoListResponse",
                  "etag": "some-etag",
                  "pageInfo": {"totalResults": 100},
                  "items": [
                    {
                      "kind": "youtube#video",
                      "id": "video-id",
                      "snippet": {
                        "title": "My Stream",
                        "liveBroadcastContent": "live",
                        "description": "Stream description"
                      },
                      "status": {
                        "uploadStatus": "succeeded",
                        "privacyStatus": "public"
                      },
                      "liveStreamingDetails": {
                        "actualStartTime": "2026-03-04T10:00:00Z",
                        "actualEndTime": "2026-03-04T12:00:00Z"
                      }
                    }
                  ]
                }""";

        YoutubeApiResponseDto response = objectMapper.readValue(json, YoutubeApiResponseDto.class);

        assertNotNull(response);
        assertNotNull(response.getItems());
        assertEquals(1, response.getItems().size());
        assertEquals("My Stream", response.getItems().getFirst().getSnippet().getTitle());
    }

    @Test
    public void testYoutubeApiResponseSerialization() throws Exception {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet("Test", "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status("succeeded");
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails =
                new YoutubeApiResponseDto.LiveStreamingDetails("2026-03-04T10:00:00Z", "2026-03-04T11:00:00Z", "2026-03-04T12:00:00Z");

        YoutubeApiResponseDto.VideoItem item = new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);
        YoutubeApiResponseDto response = new YoutubeApiResponseDto(List.of(item));

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("Test"));
        assertTrue(json.contains("live"));
        assertTrue(json.contains("succeeded"));
        assertTrue(json.contains("2026-03-04T10:00:00Z"));
    }

    @Test
    public void testMultipleVideoItems() throws Exception {
        String json = """
                {
                  "items": [
                    {
                      "snippet": {"title": "Video 1", "liveBroadcastContent": "live"},
                      "status": {"uploadStatus": "succeeded"},
                      "liveStreamingDetails": {"actualStartTime": "2026-03-04T10:00:00Z", "actualEndTime": "2026-03-04T12:00:00Z"}
                    },
                    {
                      "snippet": {"title": "Video 2", "liveBroadcastContent": "none"},
                      "status": {"uploadStatus": "succeeded"},
                      "liveStreamingDetails": {"actualStartTime": null, "actualEndTime": null}
                    }
                  ]
                }""";

        YoutubeApiResponseDto response = objectMapper.readValue(json, YoutubeApiResponseDto.class);

        assertEquals(2, response.getItems().size());
        assertEquals("Video 1", response.getItems().get(0).getSnippet().getTitle());
        assertEquals("Video 2", response.getItems().get(1).getSnippet().getTitle());
        assertEquals("live", response.getItems().get(0).getSnippet().getLiveBroadcastContent());
        assertEquals("none", response.getItems().get(1).getSnippet().getLiveBroadcastContent());
    }

    @Test
    public void testEmptyItems() throws Exception {
        String json = "{\"items\": []}";
        YoutubeApiResponseDto response = objectMapper.readValue(json, YoutubeApiResponseDto.class);

        assertNotNull(response);
        assertNotNull(response.getItems());
        assertEquals(0, response.getItems().size());
    }

    @Test
    public void testNullValues() throws Exception {
        String json = """
                {
                  "items": [
                    {
                      "snippet": {"title": null, "liveBroadcastContent": null},
                      "status": {"uploadStatus": null},
                      "liveStreamingDetails": {"actualStartTime": null, "actualEndTime": null}
                    }
                  ]
                }""";

        YoutubeApiResponseDto response = objectMapper.readValue(json, YoutubeApiResponseDto.class);

        assertNotNull(response.getItems());
        assertNull(response.getItems().getFirst().getSnippet().getTitle());
        assertNull(response.getItems().getFirst().getStatus().getUploadStatus());
        assertNull(response.getItems().getFirst().getLiveStreamingDetails().getActualStartTime());
    }

    @Test
    public void testVideoItemEquality() {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet("Title", "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status("succeeded");
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails =
                new YoutubeApiResponseDto.LiveStreamingDetails("2026-03-04T10:00:00Z", "2026-03-04T12:00:00Z", "2026-03-04T09:00:00Z");

        YoutubeApiResponseDto.VideoItem item1 = new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);
        YoutubeApiResponseDto.VideoItem item2 = new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);

        assertEquals(item1, item2);
    }

    @Test
    public void testResponseEquality() {
        YoutubeApiResponseDto.Snippet snippet = new YoutubeApiResponseDto.Snippet("Title", "live");
        YoutubeApiResponseDto.Status status = new YoutubeApiResponseDto.Status("succeeded");
        YoutubeApiResponseDto.LiveStreamingDetails liveDetails =
                new YoutubeApiResponseDto.LiveStreamingDetails("2026-03-04T10:00:00Z", "2026-03-04T12:00:00Z", "2026-03-04T09:00:00Z");

        YoutubeApiResponseDto.VideoItem item = new YoutubeApiResponseDto.VideoItem(snippet, status, liveDetails);

        YoutubeApiResponseDto response1 = new YoutubeApiResponseDto(List.of(item));
        YoutubeApiResponseDto response2 = new YoutubeApiResponseDto(List.of(item));

        assertEquals(response1, response2);
    }
}
