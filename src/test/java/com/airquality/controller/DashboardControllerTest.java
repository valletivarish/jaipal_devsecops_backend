package com.airquality.controller;

import com.airquality.dto.request.MonitoringZoneRequest;
import com.airquality.dto.request.PollutantTypeRequest;
import com.airquality.dto.request.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .name("Dashboard Zone")
                .description("Zone for dashboard tests")
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

    private Long createPollutant(String token) throws Exception {
        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("PM10")
                .description("Coarse particulate matter")
                .unit("ug/m3")
                .safeThreshold(54.0)
                .warningThreshold(154.0)
                .dangerThreshold(254.0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    @Test
    public void testGetSummary() throws Exception {
        String token = registerUserAndGetToken("summary@test.com", "summaryuser", "Summary User");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_zones").isNumber())
                .andExpect(jsonPath("$.total_active_alerts").isNumber())
                .andExpect(jsonPath("$.total_readings_today").isNumber());
    }

    @Test
    public void testGetTrends() throws Exception {
        String token = registerUserAndGetToken("trends@test.com", "trendsuser", "Trends User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant(token);

        mockMvc.perform(get("/api/dashboard/trends")
                        .header("Authorization", "Bearer " + token)
                        .param("zone_id", zoneId.toString())
                        .param("pollutant_type_id", pollutantId.toString())
                        .param("days", "30"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetComparison() throws Exception {
        String token = registerUserAndGetToken("compare@test.com", "compareuser", "Compare User");
        Long pollutantId = createPollutant(token);

        mockMvc.perform(get("/api/dashboard/comparison")
                        .header("Authorization", "Bearer " + token)
                        .param("pollutant_type_id", pollutantId.toString())
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
