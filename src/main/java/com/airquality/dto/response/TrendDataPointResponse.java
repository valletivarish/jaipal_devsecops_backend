package com.airquality.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendDataPointResponse {

    private LocalDateTime timestamp;
    private Double value;
    private Integer aqi;
}
