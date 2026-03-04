package com.h3late.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h3late.stats.dto.VideoEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class VideoEventDtoTest {

    private ObjectMapper objectMapper;
    private VideoEventDto videoEventDto;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        videoEventDto = new VideoEventDto();
    }

    @Test
    public void testVideoEventDtoCreation() {
        videoEventDto.setTitle("Test Video Title");
        assertNotNull(videoEventDto);
        assertEquals("Test Video Title", videoEventDto.getTitle());
    }

    @Test
    public void testVideoEventDtoWithConstructor() {
        VideoEventDto dto = new VideoEventDto("My Video");
        assertEquals("My Video", dto.getTitle());
    }

    @Test
    public void testVideoEventDtoSerialization() throws Exception {
        videoEventDto.setTitle("Serialization Test");
        String json = objectMapper.writeValueAsString(videoEventDto);

        assertTrue(json.contains("Serialization Test"));
        assertTrue(json.contains("title"));
    }

    @Test
    public void testVideoEventDtoDeserialization() throws Exception {
        String json = "{\"title\":\"Deserialization Test\"}";
        VideoEventDto dto = objectMapper.readValue(json, VideoEventDto.class);

        assertNotNull(dto);
        assertEquals("Deserialization Test", dto.getTitle());
    }

    @Test
    public void testVideoEventDtoDeserializationWithExtraFields() throws Exception {
        String json = "{\"title\":\"Test Video\",\"extraField\":\"ignored\",\"anotherExtra\":123}";
        VideoEventDto dto = objectMapper.readValue(json, VideoEventDto.class);

        assertNotNull(dto);
        assertEquals("Test Video", dto.getTitle());
    }

    @Test
    public void testVideoEventDtoEquality() {
        VideoEventDto dto1 = new VideoEventDto("Same Title");
        VideoEventDto dto2 = new VideoEventDto("Same Title");

        assertEquals(dto1, dto2);
    }

    @Test
    public void testVideoEventDtoInequalityWithDifferentTitle() {
        VideoEventDto dto1 = new VideoEventDto("Title 1");
        VideoEventDto dto2 = new VideoEventDto("Title 2");

        assertNotEquals(dto1, dto2);
    }

    @Test
    public void testVideoEventDtoToString() {
        videoEventDto.setTitle("ToString Test");
        String stringRepresentation = videoEventDto.toString();

        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation.contains("ToString Test"));
    }

    @Test
    public void testVideoEventDtoWithNullTitle() {
        videoEventDto.setTitle(null);
        assertNull(videoEventDto.getTitle());
    }

    @Test
    public void testVideoEventDtoWithEmptyTitle() {
        videoEventDto.setTitle("");
        assertEquals("", videoEventDto.getTitle());
    }
}
