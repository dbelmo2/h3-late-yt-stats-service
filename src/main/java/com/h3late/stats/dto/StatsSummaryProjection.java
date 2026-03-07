package com.h3late.stats.dto;

public interface StatsSummaryProjection {
    Long getTotal_streams();
    Double getAvg_lateness_minutes();
    Integer getRecord_lateness_minutes();
    Long getTotal_late_count();
    Long getTotal_early_count();
    Long getTotal_on_time_count();
}