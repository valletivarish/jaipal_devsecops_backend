package com.airquality.controller;

import com.airquality.dto.request.AlertRuleRequest;
import com.airquality.dto.request.AlertRuleUpdateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AlertRuleControllerTest {

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
                .name("Test Zone")
                .description("A test monitoring zone")
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
                .name("PM2.5")
                .description("Fine particulate matter")
                .unit("ug/m3")
                .safeThreshold(12.0)
                .warningThreshold(35.4)
                .dangerThreshold(55.4)
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
    public void testCreateAlertRule() throws Exception {
        String token = registerUserAndGetToken("alert@test.com", "alertuser", "Alert User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant(token);

        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("High PM2.5 Alert")
                .thresholdValue(50.0)
                .condition("ABOVE")
                .severity("HIGH")
                .isActive(true)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantId)
                .build();

        mockMvc.perform(post("/api/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("High PM2.5 Alert"))
                .andExpect(jsonPath("$.threshold_value").value(50.0))
                .andExpect(jsonPath("$.condition").value("ABOVE"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.zone_id").value(zoneId))
                .andExpect(jsonPath("$.pollutant_type_id").value(pollutantId));
    }

    @Test
    public void testGetAlertRules() throws Exception {
        String token = registerUserAndGetToken("rules@test.com", "rulesuser", "Rules User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant(token);

        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("Test Rule")
                .thresholdValue(30.0)
                .condition("ABOVE")
                .severity("MEDIUM")
                .isActive(true)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantId)
                .build();

        mockMvc.perform(post("/api/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Test Rule"));
    }

    @Test
    public void testGetAlertRulesByZone() throws Exception {
        String token = registerUserAndGetToken("zone@test.com", "zoneuser", "Zone User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant(token);

        AlertRuleRequest request = AlertRuleRequest.builder()
                .name("Zone Alert")
                .thresholdValue(40.0)
                .condition("ABOVE")
                .severity("LOW")
                .isActive(true)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantId)
                .build();

        mockMvc.perform(post("/api/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts/zone/" + zoneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].zone_id").value(zoneId));
    }

    @Test
    public void testUpdateAlertRule() throws Exception {
        String token = registerUserAndGetToken("update@test.com", "updateuser", "Update User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant(token);

        AlertRuleRequest createRequest = AlertRuleRequest.builder()
                .name("Original Rule")
                .thresholdValue(25.0)
                .condition("ABOVE")
                .severity("LOW")
                .isActive(true)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long ruleId = created.get("id").asLong();

        AlertRuleUpdateRequest updateRequest = AlertRuleUpdateRequest.builder()
                .name("Updated Rule")
                .thresholdValue(75.0)
                .severity("CRITICAL")
                .build();

        mockMvc.perform(put("/api/alerts/" + ruleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rule"))
                .andExpect(jsonPath("$.threshold_value").value(75.0))
                .andExpect(jsonPath("$.severity").value("CRITICAL"));
    }

    @Test
    public void testDeleteAlertRule() throws Exception {
        String token = registerUserAndGetToken("delete@test.com", "deleteuser", "Delete User");
        Long zoneId = createZone(token);
        Long pollutantId = createPollutant(token);

        AlertRuleRequest createRequest = AlertRuleRequest.builder()
                .name("To Delete")
                .thresholdValue(20.0)
                .condition("BELOW")
                .severity("MEDIUM")
                .isActive(true)
                .zoneId(zoneId)
                .pollutantTypeId(pollutantId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long ruleId = created.get("id").asLong();

        mockMvc.perform(delete("/api/alerts/" + ruleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/alerts/" + ruleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
