package com.airquality.service;

import com.airquality.model.PollutantType;
import com.airquality.repository.MonitoringZoneRepository;
import com.airquality.repository.PollutantTypeRepository;
import com.airquality.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SensorReadingServiceTest {

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private MonitoringZoneRepository monitoringZoneRepository;

    @Mock
    private PollutantTypeRepository pollutantTypeRepository;

    @InjectMocks
    private SensorReadingService sensorReadingService;

    private PollutantType pollutant;

    @BeforeEach
    void setUp() {
        pollutant = new PollutantType();
        pollutant.setId(1L);
        pollutant.setName("PM2.5");
        pollutant.setUnit("ug/m3");
        pollutant.setSafeThreshold(12.0);
        pollutant.setWarningThreshold(35.4);
        pollutant.setDangerThreshold(55.4);
    }

    @Test
    public void testCalculateAqiGoodRange() {
        // Value within safe threshold -> AQI 0-50
        int aqi = sensorReadingService.calculateAqi(6.0, pollutant);
        assertTrue(aqi >= 0 && aqi <= 50,
                "AQI should be in Good range (0-50) but was " + aqi);
    }

    @Test
    public void testCalculateAqiModerateRange() {
        // Value between safe and warning threshold -> AQI 51-100
        int aqi = sensorReadingService.calculateAqi(24.0, pollutant);
        assertTrue(aqi >= 51 && aqi <= 100,
                "AQI should be in Moderate range (51-100) but was " + aqi);
    }

    @Test
    public void testCalculateAqiUnhealthyRange() {
        // Value between warning and danger threshold -> AQI 101-200
        int aqi = sensorReadingService.calculateAqi(45.0, pollutant);
        assertTrue(aqi >= 101 && aqi <= 200,
                "AQI should be in Unhealthy range (101-200) but was " + aqi);
    }

    @Test
    public void testCalculateAqiHazardousRange() {
        // Value above danger threshold -> AQI 201-500
        int aqi = sensorReadingService.calculateAqi(80.0, pollutant);
        assertTrue(aqi >= 201 && aqi <= 500,
                "AQI should be in Hazardous range (201-500) but was " + aqi);
    }
}
