
package com.h3late.stats.repository;

import com.h3late.stats.entity.Livestream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LivestreamRepository extends JpaRepository<Livestream, String> {
    List<Livestream> findAllByOrderByScheduledStartDesc();
    List<Livestream> findByStatus(com.h3late.stats.entity.StreamStatus status);
}