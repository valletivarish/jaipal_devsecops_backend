package com.airquality.service;

import com.airquality.dto.request.AlertRuleRequest;
import com.airquality.dto.request.AlertRuleUpdateRequest;
import com.airquality.dto.response.AlertRuleResponse;
import com.airquality.exception.ForbiddenException;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.AlertRule;
import com.airquality.model.MonitoringZone;
import com.airquality.model.PollutantType;
import com.airquality.model.User;
import com.airquality.repository.AlertRuleRepository;
import com.airquality.repository.MonitoringZoneRepository;
import com.airquality.repository.PollutantTypeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final MonitoringZoneRepository monitoringZoneRepository;
    private final PollutantTypeRepository pollutantTypeRepository;

    public AlertRuleService(AlertRuleRepository alertRuleRepository,
                            MonitoringZoneRepository monitoringZoneRepository,
                            PollutantTypeRepository pollutantTypeRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.monitoringZoneRepository = monitoringZoneRepository;
        this.pollutantTypeRepository = pollutantTypeRepository;
    }

    @Transactional
    public AlertRuleResponse createAlertRule(AlertRuleRequest request, Long ownerId) {
        MonitoringZone zone = monitoringZoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", request.getZoneId()));

        PollutantType pollutantType = pollutantTypeRepository.findById(request.getPollutantTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", request.getPollutantTypeId()));

        User owner = new User();
        owner.setId(ownerId);

        AlertRule alertRule = new AlertRule();
        alertRule.setName(request.getName());
        alertRule.setThresholdValue(request.getThresholdValue());
        alertRule.setCondition(request.getCondition());
        alertRule.setSeverity(request.getSeverity());
        alertRule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        alertRule.setZone(zone);
        alertRule.setPollutantType(pollutantType);
        alertRule.setOwner(owner);

        AlertRule saved = alertRuleRepository.save(alertRule);
        return convertToResponse(saved);
    }

    public AlertRuleResponse getAlertRuleById(Long id) {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", id));
        return convertToResponse(alertRule);
    }

    public List<AlertRuleResponse> getAllAlertRules(int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return alertRuleRepository.findAll(pageable).getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<AlertRuleResponse> getAlertRulesByOwner(Long ownerId, int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return alertRuleRepository.findByOwnerId(ownerId, pageable).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<AlertRuleResponse> getAlertRulesByZone(Long zoneId, int skip, int limit) {
        int page = limit > 0 ? skip / limit : 0;
        Pageable pageable = PageRequest.of(page, limit);
        return alertRuleRepository.findByZoneId(zoneId, pageable).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlertRuleResponse updateAlertRule(Long id, AlertRuleUpdateRequest request, Long currentUserId) {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", id));

        if (!alertRule.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to update this alert rule");
        }

        if (request.getName() != null) {
            alertRule.setName(request.getName());
        }
        if (request.getThresholdValue() != null) {
            alertRule.setThresholdValue(request.getThresholdValue());
        }
        if (request.getCondition() != null) {
            alertRule.setCondition(request.getCondition());
        }
        if (request.getSeverity() != null) {
            alertRule.setSeverity(request.getSeverity());
        }
        if (request.getIsActive() != null) {
            alertRule.setIsActive(request.getIsActive());
        }
        if (request.getZoneId() != null) {
            MonitoringZone zone = monitoringZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", request.getZoneId()));
            alertRule.setZone(zone);
        }
        if (request.getPollutantTypeId() != null) {
            PollutantType pollutantType = pollutantTypeRepository.findById(request.getPollutantTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("PollutantType", request.getPollutantTypeId()));
            alertRule.setPollutantType(pollutantType);
        }

        AlertRule saved = alertRuleRepository.save(alertRule);
        return convertToResponse(saved);
    }

    @Transactional
    public void deleteAlertRule(Long id, Long currentUserId) {
        AlertRule alertRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlertRule", id));

        if (!alertRule.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to delete this alert rule");
        }

        alertRuleRepository.delete(alertRule);
    }

    private AlertRuleResponse convertToResponse(AlertRule alertRule) {
        return AlertRuleResponse.builder()
                .id(alertRule.getId())
                .name(alertRule.getName())
                .thresholdValue(alertRule.getThresholdValue())
                .condition(alertRule.getCondition())
                .severity(alertRule.getSeverity())
                .isActive(alertRule.getIsActive())
                .zoneId(alertRule.getZone().getId())
                .pollutantTypeId(alertRule.getPollutantType().getId())
                .ownerId(alertRule.getOwner().getId())
                .createdAt(alertRule.getCreatedAt())
                .updatedAt(alertRule.getUpdatedAt())
                .build();
    }
}
