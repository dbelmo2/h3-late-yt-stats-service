package com.h3late.stats.dto;

public interface StatsSummaryProjection {
    Long getTotal_streams();
    Double getTotal_late_time_minutes(); //
    Double getAvg_lateness_minutes();
    Integer getRecord_lateness_minutes();
    String getRecord_video_id();
    String getRecord_video_title();
    Long getTotal_late_count();
    Long getTotal_early_count();
    Long getTotal_on_time_count();
}