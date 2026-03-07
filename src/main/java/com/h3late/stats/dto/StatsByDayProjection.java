package com.h3late.stats.dto;

public interface StatsByDayProjection {
    String getDay_of_week();
    Integer getDay_index();
    Long getStream_count();
    Double getAvg_lateness_seconds();
}