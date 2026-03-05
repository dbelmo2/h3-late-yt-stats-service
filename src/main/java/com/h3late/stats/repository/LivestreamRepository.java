
package com.h3late.stats.repository;

import com.h3late.stats.entity.Livestream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivestreamRepository extends JpaRepository<Livestream, String>, JpaSpecificationExecutor<Livestream> {
}
