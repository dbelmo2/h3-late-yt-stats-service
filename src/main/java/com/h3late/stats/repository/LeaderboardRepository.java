package com.h3late.stats.repository;

import com.h3late.stats.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, String> {
}