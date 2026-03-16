package com.airquality.controller;

import com.airquality.dto.request.MonitoringZoneRequest;
import com.airquality.dto.request.RegisterRequest;
import com.airquality.dto.request.SensorReadingRequest;
import com.airquality.model.PollutantType;
import com.airquality.repository.PollutantTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SensorReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PollutantTypeRepository pollutantTypeRepository;

    private String authToken;
    private Long zoneId;
    private Long pollutantTypeId;

    @BeforeEach
    public void setUp() throws Exception {
        // Register a user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("sensor@test.com")
                .username("sensoruser")
                .password("StrongPass1")
                .fullName("Sensor User")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        authToken = registerJson.get("access_token").asText();

        // Create a monitoring zone
        MonitoringZoneRequest zoneRequest = MonitoringZoneRequest.builder()
                .name("Sensor Test Zone")
                .description("Zone for sensor reading tests")
                .latitude(53.3498)
                .longitude(-6.2603)
                .radius(5000.0)
                .build();

        MvcResult zoneResult = mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode zoneJson = objectMapper.readTree(zoneResult.getResponse().getContentAsString());
        zoneId = zoneJson.get("id").asLong();

        // Create a pollutant type directly in repository
        PollutantType pm25 = new PollutantType("PM2.5", "ug/m3", 12.0, 35.4, 55.4);
        PollutantType saved = pollutantTypeRepository.save(pm25);
        pollutantTypeId = saved.getId();
    }

    @Test
    public void testCreateReadingSuccess() throws Exception {
        SensorReadingRequest request = SensorReadingRequest.builder()
                .value(25.0)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantTypeId)
                .recordedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/readings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(25.0))
                .andExpect(jsonPath("$.aqi", notNullValue()))
                .andExpect(jsonPath("$.zone_id").value(zoneId))
                .andExpect(jsonPath("$.pollutant_type_id").value(pollutantTypeId));
    }

    @Test
    public void testCreateReadingInvalidZone() throws Exception {
        SensorReadingRequest request = SensorReadingRequest.builder()
                .value(25.0)
                .zoneId(99999L)
                .pollutantTypeId(pollutantTypeId)
                .recordedAt(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/readings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetReadings() throws Exception {
        // Create two readings
        for (int i = 0; i < 2; i++) {
            SensorReadingRequest request = SensorReadingRequest.builder()
                    .value(10.0 + i * 5)
                    .zoneId(zoneId)
                    .pollutantTypeId(pollutantTypeId)
                    .recordedAt(LocalDateTime.now())
                    .build();

            mockMvc.perform(post("/api/readings")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/readings")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void testGetReadingById() throws Exception {
        SensorReadingRequest request = SensorReadingRequest.builder()
                .value(30.0)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantTypeId)
                .recordedAt(LocalDateTime.now())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/readings")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long readingId = createJson.get("id").asLong();

        mockMvc.perform(get("/api/readings/" + readingId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(readingId))
                .andExpect(jsonPath("$.value").value(30.0));
    }
}
