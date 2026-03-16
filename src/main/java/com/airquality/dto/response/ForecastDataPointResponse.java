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
public class ForecastDataPointResponse {

    private String date;

    @JsonProperty("predicted_value")
    private Double predictedValue;

    @JsonProperty("lower_bound")
    private Double lowerBound;

    @JsonProperty("upper_bound")
    private Double upperBound;
}
