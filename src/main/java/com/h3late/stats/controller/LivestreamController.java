package com.h3late.stats.controller;

import com.h3late.stats.dto.StatsSummaryProjection;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import com.h3late.stats.service.LivestreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/livestream")
@RequiredArgsConstructor
class LivestreamController {

    private final LivestreamService livestreamService;

    // 1. Single Video Lookup
    @GetMapping("/{id}")
    public ResponseEntity<Livestream> getLivestreamById(@PathVariable String id) {
        return livestreamService.getLivestreamById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. The Global Search (Filters + Pagination + Sorting)
    @GetMapping
    public ResponseEntity<Page<Livestream>> searchLivestreams(
            @RequestParam(required = false) StreamStatus status,
            @RequestParam(required = false) TimeStatus timeStatus,
            @RequestParam(required = false) String search, // New parameter
            @PageableDefault(size = 20, sort = "scheduledStart", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(livestreamService.searchLivestreams(status, timeStatus, search, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsSummaryProjection> getGlobalStats() {
        return ResponseEntity.ok(livestreamService.getGlobalStats());
    }

    @GetMapping("/stats/day")
    public ResponseEntity<?> getStatsByDay() {
        return ResponseEntity.ok(livestreamService.getStatsByDay());
    }


}
