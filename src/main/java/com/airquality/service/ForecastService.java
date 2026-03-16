package com.airquality.service;

import com.airquality.dto.response.ForecastDataPointResponse;
import com.airquality.dto.response.ForecastResponse;
import com.airquality.exception.ResourceNotFoundException;
import com.airquality.model.MonitoringZone;
import com.airquality.model.PollutantType;
import com.airquality.model.SensorReading;
import com.airquality.repository.MonitoringZoneRepository;
import com.airquality.repository.PollutantTypeRepository;
import com.airquality.repository.SensorReadingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ForecastService {

    private final SensorReadingRepository sensorReadingRepository;
    private final MonitoringZoneRepository monitoringZoneRepository;
    private final PollutantTypeRepository pollutantTypeRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ForecastService(SensorReadingRepository sensorReadingRepository,
                           MonitoringZoneRepository monitoringZoneRepository,
                           PollutantTypeRepository pollutantTypeRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.monitoringZoneRepository = monitoringZoneRepository;
        this.pollutantTypeRepository = pollutantTypeRepository;
    }

    public ForecastResponse generateForecast(Long zoneId, Long pollutantTypeId,
                                              int historyDays, int forecastDays) {
        MonitoringZone zone = monitoringZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoringZone", zoneId));

        PollutantType pollutantType = pollutantTypeRepository.findById(pollutantTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("PollutantType", pollutantTypeId));

        LocalDateTime startDate = LocalDateTime.now().minusDays(historyDays);
        List<SensorReading> readings = sensorReadingRepository
                .findByZoneIdAndPollutantTypeIdAndRecordedAtAfterOrderByRecordedAtAsc(
                        zoneId, pollutantTypeId, startDate);

        if (readings.size() < 5) {
            return ForecastResponse.builder()
                    .zoneId(zone.getId().intValue())
                    .zoneName(zone.getName())
                    .pollutantName(pollutantType.getName())
                    .pollutantUnit(pollutantType.getUnit())
                    .trendDirection("INSUFFICIENT_DATA")
                    .confidenceScore(0.0)
                    .forecastData(new ArrayList<>())
                    .build();
        }

        // Convert timestamps to days since first reading
        LocalDateTime firstTimestamp = readings.get(0).getRecordedAt();
        int n = readings.size();
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = ChronoUnit.HOURS.between(firstTimestamp, readings.get(i).getRecordedAt()) / 24.0;
            y[i] = readings.get(i).getValue();
        }

        // Least-squares linear regression
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }

        double denominator = n * sumX2 - sumX * sumX;
        double slope;
        double intercept;

        if (Math.abs(denominator) < 1e-10) {
            slope = 0;
            intercept = sumY / n;
        } else {
            slope = (n * sumXY - sumX * sumY) / denominator;
            intercept = (sumY - slope * sumX) / n;
        }

        // Calculate R-squared
        double meanY = sumY / n;
        double ssTot = 0;
        double ssRes = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * x[i] + intercept;
            ssRes += (y[i] - predicted) * (y[i] - predicted);
            ssTot += (y[i] - meanY) * (y[i] - meanY);
        }

        double rSquared = ssTot > 0 ? 1.0 - (ssRes / ssTot) : 0.0;

        // Calculate residual standard deviation
        double residualStd = n > 2 ? Math.sqrt(ssRes / (n - 2)) : 0.0;

        // Determine trend direction
        String trendDirection;
        if (slope > 0.1) {
            trendDirection = "INCREASING";
        } else if (slope < -0.1) {
            trendDirection = "DECREASING";
        } else {
            trendDirection = "STABLE";
        }

        // Generate forecast data points
        double lastX = x[n - 1];
        LocalDateTime lastDate = readings.get(n - 1).getRecordedAt().toLocalDate().atStartOfDay();
        List<ForecastDataPointResponse> forecastData = new ArrayList<>();

        for (int day = 1; day <= forecastDays; day++) {
            double forecastX = lastX + day;
            double predictedValue = slope * forecastX + intercept;

            // Ensure non-negative predictions
            predictedValue = Math.max(0, predictedValue);

            double confidenceInterval = 1.96 * residualStd;
            double lowerBound = Math.max(0, predictedValue - confidenceInterval);
            double upperBound = predictedValue + confidenceInterval;

            String dateStr = lastDate.plusDays(day).format(DATE_FORMATTER);

            forecastData.add(ForecastDataPointResponse.builder()
                    .date(dateStr)
                    .predictedValue(Math.round(predictedValue * 100.0) / 100.0)
                    .lowerBound(Math.round(lowerBound * 100.0) / 100.0)
                    .upperBound(Math.round(upperBound * 100.0) / 100.0)
                    .build());
        }

        return ForecastResponse.builder()
                .zoneId(zone.getId().intValue())
                .zoneName(zone.getName())
                .pollutantName(pollutantType.getName())
                .pollutantUnit(pollutantType.getUnit())
                .trendDirection(trendDirection)
                .confidenceScore(Math.round(rSquared * 100.0) / 100.0)
                .forecastData(forecastData)
                .build();
    }
}
