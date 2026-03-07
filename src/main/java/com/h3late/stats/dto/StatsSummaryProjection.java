package com.h3late.stats.dto;

public interface StatsSummaryProjection {
    Long getTotal_streams();

    // Using Long for seconds to avoid overflow and simplify mapping
    Long getAvg_lateness_seconds();
    Long getTotal_late_time_seconds();
    Long getRecord_lateness_seconds();

    String getRecord_video_id();
    String getRecord_video_title();

    Long getTotal_late_count();
    Long getTotal_early_count();
    Long getTotal_on_time_count();
}