package com.airquality.controller;

import com.airquality.dto.request.AlertRuleRequest;
import com.airquality.dto.request.AlertRuleUpdateRequest;
import com.airquality.dto.response.AlertRuleResponse;
import com.airquality.security.UserPrincipal;
import com.airquality.service.AlertRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponse> createAlertRule(
            @Valid @RequestBody AlertRuleRequest request,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        AlertRuleResponse response = alertRuleService.createAlertRule(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> getAlertRules(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit) {
        List<AlertRuleResponse> rules = alertRuleService.getAllAlertRules(skip, limit);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/my")
    public ResponseEntity<List<AlertRuleResponse>> getMyAlertRules(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        List<AlertRuleResponse> rules = alertRuleService.getAlertRulesByOwner(currentUserId, skip, limit);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<AlertRuleResponse>> getAlertRulesByZone(
            @PathVariable Long zoneId,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit) {
        List<AlertRuleResponse> rules = alertRuleService.getAlertRulesByZone(zoneId, skip, limit);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<AlertRuleResponse> getAlertRuleById(@PathVariable Long ruleId) {
        AlertRuleResponse rule = alertRuleService.getAlertRuleById(ruleId);
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<AlertRuleResponse> updateAlertRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody AlertRuleUpdateRequest request,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        AlertRuleResponse rule = alertRuleService.updateAlertRule(ruleId, request, currentUserId);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteAlertRule(
            @PathVariable Long ruleId,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        alertRuleService.deleteAlertRule(ruleId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }
}
