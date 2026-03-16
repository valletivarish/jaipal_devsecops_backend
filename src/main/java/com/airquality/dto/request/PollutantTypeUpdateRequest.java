package com.airquality.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PollutantTypeUpdateRequest {

    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    private String description;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    @Positive(message = "Safe threshold must be positive")
    @JsonProperty("safe_threshold")
    private Double safeThreshold;

    @Positive(message = "Warning threshold must be positive")
    @JsonProperty("warning_threshold")
    private Double warningThreshold;

    @Positive(message = "Danger threshold must be positive")
    @JsonProperty("danger_threshold")
    private Double dangerThreshold;
}
