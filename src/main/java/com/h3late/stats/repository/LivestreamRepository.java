
package com.h3late.stats.repository;

import com.h3late.stats.dto.StatsByDayProjection;
import com.h3late.stats.dto.StatsSummaryProjection;
import com.h3late.stats.entity.Livestream;
import com.h3late.stats.entity.StreamStatus;
import com.h3late.stats.entity.TimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivestreamRepository extends JpaRepository<Livestream, String>, JpaSpecificationExecutor<Livestream> {
    @Query(value = "SELECT * FROM h3_global_stats", nativeQuery = true)
    StatsSummaryProjection getGlobalStats();

    @Query(value = "SELECT * FROM h3_stats_by_day", nativeQuery = true)
    List<StatsByDayProjection> getStatsByDay();

    @Query("SELECT l FROM Livestream l WHERE " +
            "(l.status = :status OR :status IS NULL) AND " +
            "(l.timeStatus = :timeStatus OR :timeStatus IS NULL) AND " +
            "(:search IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.videoId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Livestream> searchAdvanced(
            @Param("status") StreamStatus status,
            @Param("timeStatus") TimeStatus timeStatus,
            @Param("search") String search,
            Pageable pageable);

}
