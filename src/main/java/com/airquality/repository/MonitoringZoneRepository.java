package com.airquality.repository;

import com.airquality.model.MonitoringZone;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoringZoneRepository extends JpaRepository<MonitoringZone, Long> {

    List<MonitoringZone> findByOwnerId(Long ownerId, Pageable pageable);
}
