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
public class PollutantTypeResponse {

    private Long id;
    private String name;
    private String description;
    private String unit;

    @JsonProperty("safe_threshold")
    private Double safeThreshold;

    @JsonProperty("warning_threshold")
    private Double warningThreshold;

    @JsonProperty("danger_threshold")
    private Double dangerThreshold;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
