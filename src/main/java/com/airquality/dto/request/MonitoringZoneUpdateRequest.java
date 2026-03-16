package com.airquality.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringZoneUpdateRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String description;

    @DecimalMin(value = "-90", message = "Latitude must be at least -90")
    @DecimalMax(value = "90", message = "Latitude must be at most 90")
    private Double latitude;

    @DecimalMin(value = "-180", message = "Longitude must be at least -180")
    @DecimalMax(value = "180", message = "Longitude must be at most 180")
    private Double longitude;

    @Positive(message = "Radius must be positive")
    @DecimalMax(value = "100000", message = "Radius must not exceed 100000")
    private Double radius;
}
