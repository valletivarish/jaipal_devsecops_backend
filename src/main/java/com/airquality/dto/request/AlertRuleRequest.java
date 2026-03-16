package com.airquality.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class AlertRuleRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Threshold value is required")
    @Positive(message = "Threshold value must be positive")
    @JsonProperty("threshold_value")
    private Double thresholdValue;

    @NotBlank(message = "Condition is required")
    @Pattern(regexp = "^(ABOVE|BELOW)$", message = "Condition must be ABOVE or BELOW")
    private String condition;

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "Severity must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String severity;

    @JsonProperty("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @NotNull(message = "Zone ID is required")
    @JsonProperty("zone_id")
    private Long zoneId;

    @NotNull(message = "Pollutant type ID is required")
    @JsonProperty("pollutant_type_id")
    private Long pollutantTypeId;
}
