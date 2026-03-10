package com.h3late.stats.repository;

import com.h3late.stats.entity.LeaderboardEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, String> {
    @Query("SELECT e FROM LeaderboardEntry e WHERE " +
            "(:search IS NULL OR LOWER(e.userName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<LeaderboardEntry> searchLeaderboard(
            @Param("search") String search,
            Pageable pageable);

}