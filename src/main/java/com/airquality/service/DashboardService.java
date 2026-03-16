package com.airquality.service;

import com.airquality.dto.response.DashboardSummaryResponse;
import com.airquality.dto.response.PollutantTrendResponse;
import com.airquality.dto.response.TrendDataPointResponse;
import com.airquality.dto.response.ZoneComparisonResponse;
import com.airquality.dto.response.ZoneSummaryResponse;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.MonitoringZone;
import com.airquality.model.PollutantType;
import com.airquality.model.SensorReading;
import com.airquality.repository.AlertRuleRepository;
import com.airquality.repository.MonitoringZoneRepository;
import com.airquality.repository.PollutantTypeRepository;
import com.airquality.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final MonitoringZoneRepository monitoringZoneRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final PollutantTypeRepository pollutantTypeRepository;

    public DashboardService(MonitoringZoneRepository monitoringZoneRepository,
                            SensorReadingRepository sensorReadingRepository,
                            AlertRuleRepository alertRuleRepository,
                            PollutantTypeRepository pollutantTypeRepository) {
        this.monitoringZoneRepository = monitoringZoneRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.pollutantTypeRepository = pollutantTypeRepository;
    }

    public DashboardSummaryResponse getDashboardSummary() {
        int totalZones = (int) monitoringZoneRepository.count();
        int totalActiveAlerts = (int) alertRuleRepository.countByIsActiveTrue();
        int totalReadingsToday = (int) sensorReadingRepository.countReadingsToday();
        Double averageAqi = sensorReadingRepository.getAverageAqiToday();

        List<MonitoringZone> zones = monitoringZoneRepository.findAll();
        List<ZoneSummaryResponse> zoneSummaries = new ArrayList<>();

        for (MonitoringZone zone : zones) {
            List<SensorReading> latestReadings = sensorReadingRepository
                    .findByZoneIdOrderByRecordedAtDesc(zone.getId(), PageRequest.of(0, 1));

            Integer latestAqi = null;
            String aqiCategory = "N/A";
            if (!latestReadings.isEmpty() && latestReadings.get(0).getAqi() != null) {
                latestAqi = latestReadings.get(0).getAqi();
                aqiCategory = getAqiCategory(latestAqi);
            }

            int activeAlerts = (int) alertRuleRepository.countByZoneIdAndIsActiveTrue(zone.getId());
            int totalReadings = zone.getSensorReadings().size();

            zoneSummaries.add(ZoneSummaryResponse.builder()
                    .zoneId(zone.getId().intValue())
                    .zoneName(zone.getName())
                    .latestAqi(latestAqi)
                    .aqiCategory(aqiCategory)
                    .activeAlerts(activeAlerts)
                    .totalReadings(totalReadings)
                    .build());
        }

        return DashboardSummaryResponse.builder()
                .totalZones(totalZones)
                .totalActiveAlerts(totalActiveAlerts)
                .totalReadingsToday(totalReadingsToday)
                .averageAqi(averageAqi)
                .zoneSummaries(zoneSummaries)
                .build();
    }

    public PollutantTrendResponse getPollutantTrend(Long zoneId, Long pollutantTypeId, int days) {
        MonitoringZone zone = monitoringZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", zoneId));

        PollutantType pollutantType = pollutantTypeRepository.findById(pollutantTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", pollutantTypeId));

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<SensorReading> readings = sensorReadingRepository
                .findByZoneIdAndPollutantTypeIdAndRecordedAtAfterOrderByRecordedAtAsc(
                        zoneId, pollutantTypeId, startDate);

        List<TrendDataPointResponse> dataPoints = readings.stream()
                .map(reading -> TrendDataPointResponse.builder()
                        .timestamp(reading.getRecordedAt())
                        .value(reading.getValue())
                        .aqi(reading.getAqi())
                        .build())
                .collect(Collectors.toList());

        return PollutantTrendResponse.builder()
                .zoneId(zone.getId().intValue())
                .zoneName(zone.getName())
                .pollutantName(pollutantType.getName())
                .pollutantUnit(pollutantType.getUnit())
                .dataPoints(dataPoints)
                .build();
    }

    public List<ZoneComparisonResponse> getZoneComparison(Long pollutantTypeId, int days) {
        PollutantType pollutantType = pollutantTypeRepository.findById(pollutantTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", pollutantTypeId));

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = sensorReadingRepository.getZoneComparison(pollutantTypeId, startDate);

        return results.stream()
                .map(row -> ZoneComparisonResponse.builder()
                        .zoneId(((Number) row[0]).intValue())
                        .zoneName((String) row[1])
                        .pollutantName(pollutantType.getName())
                        .averageValue(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                        .maxValue(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                        .minValue(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                        .readingCount(((Number) row[5]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    String getAqiCategory(int aqi) {
        if (aqi <= 50) {
            return "Good";
        } else if (aqi <= 100) {
            return "Moderate";
        } else if (aqi <= 150) {
            return "Unhealthy for Sensitive Groups";
        } else if (aqi <= 200) {
            return "Unhealthy";
        } else if (aqi <= 300) {
            return "Very Unhealthy";
        } else {
            return "Hazardous";
        }
    }
}
