package com.airquality.controller;

import com.airquality.dto.request.MonitoringZoneRequest;
import com.airquality.dto.request.RegisterRequest;
import com.airquality.dto.request.SensorReadingRequest;
import com.airquality.model.PollutantType;
import com.airquality.repository.PollutantTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ForecastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PollutantTypeRepository pollutantTypeRepository;

    private String registerUserAndGetToken(String email, String username, String fullName) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .username(username)
                .password("StrongPass1")
                .fullName(fullName)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("access_token").asText();
    }

    private Long createZone(String token) throws Exception {
        MonitoringZoneRequest request = MonitoringZoneRequest.builder()
                .name("Forecast Zone")
                .description("Zone for forecast tests")
                .latitude(53.3498)
                .longitude(-6.2603)
                .radius(5000.0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private Long createPollutant() {
        PollutantType pm25 = new PollutantType("PM2.5", "ug/m3", 12.0, 35.4, 55.4);
        PollutantType saved = pollutantTypeRepository.save(pm25);
        return saved.getId();
    }

    private void addSensorReadings(String token, Long zoneId, Long pollutantTypeId, int count) throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().minusDays(count);
        for (int i = 0; i < count; i++) {
            SensorReadingRequest request = SensorReadingRequest.builder()
                    .value(20.0 + i * 3.5)
                    .zoneId(zoneId)
                    .pollutantTypeId(pollutantTypeId)
                    .recordedAt(baseTime.plusDays(i))
                    .build();

            mockMvc.perform(post("/api/readings")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    public void testGenerateForecastSuccess() throws Exception {
        String token = registerUserAndGetToken("forecast@test.com", "forecastuser", "Forecast User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant();
        addSensorReadings(token, zoneId, pollutantId, 7);

        mockMvc.perform(get("/api/forecast")
                        .header("Authorization", "Bearer " + token)
                        .param("zone_id", zoneId.toString())
                        .param("pollutant_type_id", pollutantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone_id").value(zoneId))
                .andExpect(jsonPath("$.zone_name").value("Forecast Zone"))
                .andExpect(jsonPath("$.pollutant_name").value("PM2.5"))
                .andExpect(jsonPath("$.pollutant_unit").value("ug/m3"))
                .andExpect(jsonPath("$.trend_direction").isString())
                .andExpect(jsonPath("$.confidence_score").isNumber())
                .andExpect(jsonPath("$.forecast_data").isArray());
    }

    @Test
    public void testGenerateForecastNonExistentZone() throws Exception {
        String token = registerUserAndGetToken("forecast404@test.com", "forecast404user", "Forecast 404 User");
        Long pollutantId = createPollutant();

        mockMvc.perform(get("/api/forecast")
                        .header("Authorization", "Bearer " + token)
                        .param("zone_id", "99999")
                        .param("pollutant_type_id", pollutantId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGenerateForecastWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/forecast")
                        .param("zone_id", "1")
                        .param("pollutant_type_id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGenerateForecastWithCustomParams() throws Exception {
        String token = registerUserAndGetToken("forecastcustom@test.com", "forecastcustomuser", "Forecast Custom User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant();
        addSensorReadings(token, zoneId, pollutantId, 10);

        mockMvc.perform(get("/api/forecast")
                        .header("Authorization", "Bearer " + token)
                        .param("zone_id", zoneId.toString())
                        .param("pollutant_type_id", pollutantId.toString())
                        .param("history_days", "14")
                        .param("forecast_days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone_id").value(zoneId))
                .andExpect(jsonPath("$.pollutant_name").value("PM2.5"))
                .andExpect(jsonPath("$.trend_direction").isString())
                .andExpect(jsonPath("$.confidence_score").isNumber())
                .andExpect(jsonPath("$.forecast_data").isArray());
    }
}
