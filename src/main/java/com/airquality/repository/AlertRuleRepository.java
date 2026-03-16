package com.airquality.repository;

import com.airquality.model.AlertRule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByOwnerId(Long ownerId, Pageable pageable);

    List<AlertRule> findByZoneId(Long zoneId, Pageable pageable);

    long countByIsActiveTrue();

    long countByZoneIdAndIsActiveTrue(Long zoneId);
}
