package com.airquality.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneSummaryResponse {

    @JsonProperty("zone_id")
    private int zoneId;

    @JsonProperty("zone_name")
    private String zoneName;

    @JsonProperty("latest_aqi")
    private Integer latestAqi;

    @JsonProperty("aqi_category")
    private String aqiCategory;

    @JsonProperty("active_alerts")
    private int activeAlerts;

    @JsonProperty("total_readings")
    private int totalReadings;
}
