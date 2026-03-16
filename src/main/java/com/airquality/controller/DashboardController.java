package com.airquality.controller;

import com.airquality.dto.response.DashboardSummaryResponse;
import com.airquality.dto.response.PollutantTrendResponse;
import com.airquality.dto.response.ZoneComparisonResponse;
import com.airquality.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/trends")
    public ResponseEntity<PollutantTrendResponse> getTrends(
            @RequestParam(name = "zone_id") Long zoneId,
            @RequestParam(name = "pollutant_type_id") Long pollutantTypeId,
            @RequestParam(defaultValue = "30") int days) {
        PollutantTrendResponse trends = dashboardService.getPollutantTrend(zoneId, pollutantTypeId, days);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/comparison")
    public ResponseEntity<List<ZoneComparisonResponse>> getComparison(
            @RequestParam(name = "pollutant_type_id") Long pollutantTypeId,
            @RequestParam(defaultValue = "7") int days) {
        List<ZoneComparisonResponse> comparison = dashboardService.getZoneComparison(pollutantTypeId, days);
        return ResponseEntity.ok(comparison);
    }
}
