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
public class ForecastResponse {

    @JsonProperty("zone_id")
    private int zoneId;

    @JsonProperty("zone_name")
    private String zoneName;

    @JsonProperty("pollutant_name")
    private String pollutantName;

    @JsonProperty("pollutant_unit")
    private String pollutantUnit;

    @JsonProperty("trend_direction")
    private String trendDirection;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @JsonProperty("forecast_data")
    private List<ForecastDataPointResponse> forecastData;
}
