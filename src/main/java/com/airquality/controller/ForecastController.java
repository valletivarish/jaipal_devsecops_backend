package com.airquality.controller;

import com.airquality.dto.response.ForecastResponse;
import com.airquality.service.ForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public ResponseEntity<ForecastResponse> getForecast(
            @RequestParam(name = "zone_id") Long zoneId,
            @RequestParam(name = "pollutant_type_id") Long pollutantTypeId,
            @RequestParam(name = "history_days", defaultValue = "30") int historyDays,
            @RequestParam(name = "forecast_days", defaultValue = "7") int forecastDays) {
        ForecastResponse forecast = forecastService.generateForecast(zoneId, pollutantTypeId, historyDays, forecastDays);
        return ResponseEntity.ok(forecast);
    }
}
