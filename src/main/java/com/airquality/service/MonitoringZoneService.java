package com.airquality.service;

import com.airquality.dto.request.MonitoringZoneRequest;
import com.airquality.dto.request.MonitoringZoneUpdateRequest;
import com.airquality.dto.response.MonitoringZoneResponse;
import com.airquality.exception.ForbiddenException;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.MonitoringZone;
import com.airquality.model.User;
import com.airquality.repository.MonitoringZoneRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MonitoringZoneService {

    private final MonitoringZoneRepository monitoringZoneRepository;

    public MonitoringZoneService(MonitoringZoneRepository monitoringZoneRepository) {
        this.monitoringZoneRepository = monitoringZoneRepository;
    }

    @Transactional
    public MonitoringZoneResponse createZone(MonitoringZoneRequest request, Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);

        MonitoringZone zone = new MonitoringZone();
        zone.setName(request.getName());
        zone.setDescription(request.getDescription());
        zone.setLatitude(request.getLatitude());
        zone.setLongitude(request.getLongitude());
        zone.setRadius(request.getRadius());
        zone.setStatus("ACTIVE");
        zone.setOwner(owner);

        MonitoringZone savedZone = monitoringZoneRepository.save(zone);
        return convertToResponse(savedZone);
    }

    public MonitoringZoneResponse getZoneById(Long id) {
        MonitoringZone zone = monitoringZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", id));
        return convertToResponse(zone);
    }

    public List<MonitoringZoneResponse> getAllZones(int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return monitoringZoneRepository.findAll(pageable).getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MonitoringZoneResponse> getZonesByOwner(Long ownerId, int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return monitoringZoneRepository.findByOwnerId(ownerId, pageable).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MonitoringZoneResponse updateZone(Long id, MonitoringZoneUpdateRequest request, Long currentUserId) {
        MonitoringZone zone = monitoringZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", id));

        if (!zone.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to update this zone");
        }

        if (request.getName() != null) {
            zone.setName(request.getName());
        }
        if (request.getDescription() != null) {
            zone.setDescription(request.getDescription());
        }
        if (request.getLatitude() != null) {
            zone.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            zone.setLongitude(request.getLongitude());
        }
        if (request.getRadius() != null) {
            zone.setRadius(request.getRadius());
        }

        MonitoringZone savedZone = monitoringZoneRepository.save(zone);
        return convertToResponse(savedZone);
    }

    @Transactional
    public void deleteZone(Long id, Long currentUserId) {
        MonitoringZone zone = monitoringZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", id));

        if (!zone.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to delete this zone");
        }

        monitoringZoneRepository.delete(zone);
    }

    private MonitoringZoneResponse convertToResponse(MonitoringZone zone) {
        return MonitoringZoneResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .description(zone.getDescription())
                .latitude(zone.getLatitude())
                .longitude(zone.getLongitude())
                .radius(zone.getRadius())
                .status(zone.getStatus())
                .ownerId(zone.getOwner().getId())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}
