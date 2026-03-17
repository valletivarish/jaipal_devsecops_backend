package com.airquality.controller;

import com.airquality.dto.request.SensorReadingRequest;
import com.airquality.dto.response.SensorReadingResponse;
import com.airquality.service.SensorReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/readings")
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    public SensorReadingController(SensorReadingService sensorReadingService) {
        this.sensorReadingService = sensorReadingService;
    }

    @PostMapping
    public ResponseEntity<SensorReadingResponse> createReading(
            @Valid @RequestBody SensorReadingRequest request) {
        SensorReadingResponse response = sensorReadingService.createReading(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SensorReadingResponse>> getReadings(
            @RequestParam(name = "zone_id", required = false) Long zoneId,
            @RequestParam(name = "pollutant_type_id", required = false) Long pollutantTypeId,
            @RequestParam(name = "start_date", required = false) LocalDateTime startDate,
            @RequestParam(name = "end_date", required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir) {
        List<SensorReadingResponse> readings = sensorReadingService.getReadings(
                zoneId, pollutantTypeId, startDate, endDate, skip, limit, sortBy, sortDir);
        return ResponseEntity.ok(readings);
    }

    @GetMapping("/{readingId}")
    public ResponseEntity<SensorReadingResponse> getReadingById(@PathVariable Long readingId) {
        SensorReadingResponse reading = sensorReadingService.getReadingById(readingId);
        return ResponseEntity.ok(reading);
    }
}
