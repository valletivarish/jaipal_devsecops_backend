package com.airquality.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingResponse {

    private Long id;
    private Double value;
    private Integer aqi;

    @JsonProperty("recorded_at")
    private LocalDateTime recordedAt;

    @JsonProperty("zone_id")
    private Long zoneId;

    @JsonProperty("pollutant_type_id")
    private Long pollutantTypeId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
