package com.airquality.service;

import com.airquality.dto.request.SensorReadingRequest;
import com.airquality.dto.response.SensorReadingResponse;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.MonitoringZone;
import com.airquality.model.PollutantType;
import com.airquality.model.SensorReading;
import com.airquality.repository.MonitoringZoneRepository;
import com.airquality.repository.PollutantTypeRepository;
import com.airquality.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final MonitoringZoneRepository monitoringZoneRepository;
    private final PollutantTypeRepository pollutantTypeRepository;

    public SensorReadingService(SensorReadingRepository sensorReadingRepository,
                                MonitoringZoneRepository monitoringZoneRepository,
                                PollutantTypeRepository pollutantTypeRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.monitoringZoneRepository = monitoringZoneRepository;
        this.pollutantTypeRepository = pollutantTypeRepository;
    }

    @Transactional
    public SensorReadingResponse createReading(SensorReadingRequest request) {
        MonitoringZone zone = monitoringZoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", request.getZoneId()));

        PollutantType pollutantType = pollutantTypeRepository.findById(request.getPollutantTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", request.getPollutantTypeId()));

        int calculatedAqi = calculateAqi(request.getValue(), pollutantType);

        SensorReading reading = new SensorReading();
        reading.setValue(request.getValue());
        reading.setAqi(calculatedAqi);
        reading.setRecordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now());
        reading.setZone(zone);
        reading.setPollutantType(pollutantType);

        SensorReading saved = sensorReadingRepository.save(reading);
        return convertToResponse(saved);
    }

    public SensorReadingResponse getReadingById(Long id) {
        SensorReading reading = sensorReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SensorReading", id));
        return convertToResponse(reading);
    }

    public List<SensorReadingResponse> getReadings(Long zoneId, Long pollutantTypeId,
                                                    LocalDateTime startDate, LocalDateTime endDate,
                                                    int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return sensorReadingRepository.findByFilters(zoneId, pollutantTypeId, startDate, endDate, pageable).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    int calculateAqi(double value, PollutantType pollutant) {
        double safeThreshold = pollutant.getSafeThreshold();
        double warningThreshold = pollutant.getWarningThreshold();
        double dangerThreshold = pollutant.getDangerThreshold();

        if (value <= safeThreshold) {
            double ratio = safeThreshold > 0 ? value / safeThreshold : 0;
            return (int) (ratio * 50);
        } else if (value <= warningThreshold) {
            double rangeSize = warningThreshold - safeThreshold;
            double ratio = rangeSize > 0 ? (value - safeThreshold) / rangeSize : 0;
            return (int) (51 + ratio * 49);
        } else if (value <= dangerThreshold) {
            double rangeSize = dangerThreshold - warningThreshold;
            double ratio = rangeSize > 0 ? (value - warningThreshold) / rangeSize : 0;
            return (int) (101 + ratio * 99);
        } else {
            double overshoot = value - dangerThreshold;
            double ratio = Math.min(dangerThreshold > 0 ? overshoot / dangerThreshold : 1.0, 1.0);
            return (int) (201 + ratio * 299);
        }
    }

    private SensorReadingResponse convertToResponse(SensorReading reading) {
        return SensorReadingResponse.builder()
                .id(reading.getId())
                .value(reading.getValue())
                .aqi(reading.getAqi())
                .recordedAt(reading.getRecordedAt())
                .zoneId(reading.getZone().getId())
                .pollutantTypeId(reading.getPollutantType().getId())
                .createdAt(reading.getCreatedAt())
                .build();
    }
}
