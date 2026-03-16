package com.airquality.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PollutantTypeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    private String description;

    @NotBlank(message = "Unit is required")
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    @NotNull(message = "Safe threshold is required")
    @Positive(message = "Safe threshold must be positive")
    @JsonProperty("safe_threshold")
    private Double safeThreshold;

    @NotNull(message = "Warning threshold is required")
    @Positive(message = "Warning threshold must be positive")
    @JsonProperty("warning_threshold")
    private Double warningThreshold;

    @NotNull(message = "Danger threshold is required")
    @Positive(message = "Danger threshold must be positive")
    @JsonProperty("danger_threshold")
    private Double dangerThreshold;
}
