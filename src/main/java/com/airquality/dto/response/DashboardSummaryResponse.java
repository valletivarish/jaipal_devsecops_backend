package com.airquality.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    @JsonProperty("total_zones")
    private int totalZones;

    @JsonProperty("total_active_alerts")
    private int totalActiveAlerts;

    @JsonProperty("total_readings_today")
    private int totalReadingsToday;

    @JsonProperty("average_aqi")
    private Double averageAqi;

    @JsonProperty("zone_summaries")
    private List<ZoneSummaryResponse> zoneSummaries;
}
