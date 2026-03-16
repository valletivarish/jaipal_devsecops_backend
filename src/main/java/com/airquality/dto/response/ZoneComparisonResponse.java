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
public class ZoneComparisonResponse {

    @JsonProperty("zone_id")
    private int zoneId;

    @JsonProperty("zone_name")
    private String zoneName;

    @JsonProperty("pollutant_name")
    private String pollutantName;

    @JsonProperty("average_value")
    private Double averageValue;

    @JsonProperty("max_value")
    private Double maxValue;

    @JsonProperty("min_value")
    private Double minValue;

    @JsonProperty("reading_count")
    private int readingCount;
}
