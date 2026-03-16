package com.airquality.controller;

import com.airquality.dto.request.MonitoringZoneRequest;
import com.airquality.dto.request.MonitoringZoneUpdateRequest;
import com.airquality.dto.response.MonitoringZoneResponse;
import com.airquality.security.UserPrincipal;
import com.airquality.service.MonitoringZoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class MonitoringZoneController {

    private final MonitoringZoneService monitoringZoneService;

    public MonitoringZoneController(MonitoringZoneService monitoringZoneService) {
        this.monitoringZoneService = monitoringZoneService;
    }

    @PostMapping
    public ResponseEntity<MonitoringZoneResponse> createZone(
            @Valid @RequestBody MonitoringZoneRequest request,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        MonitoringZoneResponse response = monitoringZoneService.createZone(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MonitoringZoneResponse>> getZones(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit) {
        List<MonitoringZoneResponse> zones = monitoringZoneService.getAllZones(skip, limit);
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MonitoringZoneResponse>> getMyZones(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        List<MonitoringZoneResponse> zones = monitoringZoneService.getZonesByOwner(currentUserId, skip, limit);
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/{zoneId}")
    public ResponseEntity<MonitoringZoneResponse> getZoneById(@PathVariable Long zoneId) {
        MonitoringZoneResponse zone = monitoringZoneService.getZoneById(zoneId);
        return ResponseEntity.ok(zone);
    }

    @PutMapping("/{zoneId}")
    public ResponseEntity<MonitoringZoneResponse> updateZone(
            @PathVariable Long zoneId,
            @Valid @RequestBody MonitoringZoneUpdateRequest request,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        MonitoringZoneResponse zone = monitoringZoneService.updateZone(zoneId, request, currentUserId);
        return ResponseEntity.ok(zone);
    }

    @DeleteMapping("/{zoneId}")
    public ResponseEntity<Void> deleteZone(
            @PathVariable Long zoneId,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        monitoringZoneService.deleteZone(zoneId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
