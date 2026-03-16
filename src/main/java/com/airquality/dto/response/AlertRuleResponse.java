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
public class AlertRuleResponse {

    private Long id;
    private String name;

    @JsonProperty("threshold_value")
    private Double thresholdValue;

    private String condition;
    private String severity;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("zone_id")
    private Long zoneId;

    @JsonProperty("pollutant_type_id")
    private Long pollutantTypeId;

    @JsonProperty("owner_id")
    private Long ownerId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
