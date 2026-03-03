package com.h3late.stats.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "livestream")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livestream {
    @Id
    private String videoId;
    private String title;
    private Instant scheduledStart;
    private Instant actualStart;
    private Instant actualEnd;
    private Long diffSeconds;
    private Long totalDurationSeconds;
    @Enumerated(EnumType.STRING)
    private StreamStatus status;
    @Enumerated(EnumType.STRING)
    private TimeStatus timeStatus;
    @Column(updatable = false)
    private Instant createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}