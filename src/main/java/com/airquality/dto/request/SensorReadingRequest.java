package com.airquality.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingRequest {

    @NotNull(message = "Value is required")
    @PositiveOrZero(message = "Value must be zero or positive")
    private Double value;

    @Min(value = 0, message = "AQI must be at least 0")
    @Max(value = 500, message = "AQI must not exceed 500")
    private Integer aqi;

    @JsonProperty("recorded_at")
    private LocalDateTime recordedAt;

    @NotNull(message = "Zone ID is required")
    @JsonProperty("zone_id")
    private Long zoneId;

    @NotNull(message = "Pollutant type ID is required")
    @JsonProperty("pollutant_type_id")
    private Long pollutantTypeId;
}
