package com.airquality.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AlertRuleUpdateRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Positive(message = "Threshold value must be positive")
    @JsonProperty("threshold_value")
    private Double thresholdValue;

    @Pattern(regexp = "^(ABOVE|BELOW)$", message = "Condition must be ABOVE or BELOW")
    private String condition;

    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "Severity must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String severity;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("zone_id")
    private Long zoneId;

    @JsonProperty("pollutant_type_id")
    private Long pollutantTypeId;
}
