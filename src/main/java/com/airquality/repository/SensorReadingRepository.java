package com.airquality.repository;

import com.airquality.model.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByZoneIdAndPollutantTypeIdAndRecordedAtAfterOrderByRecordedAtAsc(
            Long zoneId, Long pollutantTypeId, LocalDateTime after);

    List<SensorReading> findByZoneIdOrderByRecordedAtDesc(Long zoneId, Pageable pageable);

    @Query(value = "SELECT sr.* FROM sensor_readings sr WHERE " +
            "(CAST(:zoneId AS BIGINT) IS NULL OR sr.zone_id = :zoneId) AND " +
            "(CAST(:pollutantTypeId AS BIGINT) IS NULL OR sr.pollutant_type_id = :pollutantTypeId) AND " +
            "(CAST(:startDate AS TIMESTAMP) IS NULL OR sr.recorded_at >= :startDate) AND " +
            "(CAST(:endDate AS TIMESTAMP) IS NULL OR sr.recorded_at <= :endDate) " +
            "ORDER BY sr.recorded_at DESC",
            nativeQuery = true)
    List<SensorReading> findByFilters(
            @Param("zoneId") Long zoneId,
            @Param("pollutantTypeId") Long pollutantTypeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT COUNT(sr) FROM SensorReading sr WHERE sr.recordedAt >= CURRENT_DATE")
    long countReadingsToday();

    @Query("SELECT AVG(sr.aqi) FROM SensorReading sr WHERE sr.recordedAt >= CURRENT_DATE AND sr.aqi IS NOT NULL")
    Double getAverageAqiToday();

    @Query("SELECT sr.zone.id, sr.zone.name, AVG(sr.value), MAX(sr.value), MIN(sr.value), COUNT(sr) " +
            "FROM SensorReading sr " +
            "WHERE sr.pollutantType.id = :pollutantTypeId AND sr.recordedAt >= :startDate " +
            "GROUP BY sr.zone.id, sr.zone.name")
    List<Object[]> getZoneComparison(
            @Param("pollutantTypeId") Long pollutantTypeId,
            @Param("startDate") LocalDateTime startDate);
}
