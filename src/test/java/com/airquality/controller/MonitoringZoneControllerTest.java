package com.airquality.controller;

import com.airquality.dto.request.MonitoringZoneRequest;
import com.airquality.dto.request.MonitoringZoneUpdateRequest;
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
public class MonitoringZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerUserAndGetToken(String email, String username) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .username(username)
                .password("StrongPass1")
                .fullName("Test User")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("access_token").asText();
    }

    private Long createZoneAndGetId(String token, String name, double lat, double lon) throws Exception {
        MonitoringZoneRequest request = MonitoringZoneRequest.builder()
                .name(name)
                .description("Test zone description")
                .latitude(lat)
                .longitude(lon)
                .radius(1000.0)
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

    @Test
    public void testCreateZoneSuccess() throws Exception {
        String token = registerUserAndGetToken("zone@test.com", "zoneuser");

        MonitoringZoneRequest request = MonitoringZoneRequest.builder()
                .name("Test Zone")
                .description("A test monitoring zone")
                .latitude(53.3498)
                .longitude(-6.2603)
                .radius(5000.0)
                .build();

        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Zone"))
                .andExpect(jsonPath("$.latitude").value(53.3498))
                .andExpect(jsonPath("$.longitude").value(-6.2603))
                .andExpect(jsonPath("$.radius").value(5000.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void testGetAllZones() throws Exception {
        String token = registerUserAndGetToken("allzones@test.com", "allzonesuser");
        createZoneAndGetId(token, "Zone A", 10.0, 20.0);
        createZoneAndGetId(token, "Zone B", 30.0, 40.0);

        mockMvc.perform(get("/api/zones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void testGetMyZones() throws Exception {
        String token1 = registerUserAndGetToken("my1@test.com", "myuser1");
        String token2 = registerUserAndGetToken("my2@test.com", "myuser2");

        createZoneAndGetId(token1, "My Zone 1", 10.0, 20.0);
        createZoneAndGetId(token1, "My Zone 2", 30.0, 40.0);
        createZoneAndGetId(token2, "Other Zone", 50.0, 60.0);

        mockMvc.perform(get("/api/zones/my")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void testGetZoneById() throws Exception {
        String token = registerUserAndGetToken("byid@test.com", "byiduser");
        Long zoneId = createZoneAndGetId(token, "Specific Zone", 12.0, 34.0);

        mockMvc.perform(get("/api/zones/" + zoneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(zoneId))
                .andExpect(jsonPath("$.name").value("Specific Zone"));
    }

    @Test
    public void testUpdateZone() throws Exception {
        String token = registerUserAndGetToken("update@test.com", "updateuser");
        Long zoneId = createZoneAndGetId(token, "Original Zone", 10.0, 20.0);

        MonitoringZoneUpdateRequest updateRequest = MonitoringZoneUpdateRequest.builder()
                .name("Updated Zone")
                .latitude(15.0)
                .build();

        mockMvc.perform(put("/api/zones/" + zoneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Zone"))
                .andExpect(jsonPath("$.latitude").value(15.0));
    }

    @Test
    public void testDeleteZone() throws Exception {
        String token = registerUserAndGetToken("delete@test.com", "deleteuser");
        Long zoneId = createZoneAndGetId(token, "Delete Zone", 10.0, 20.0);

        mockMvc.perform(delete("/api/zones/" + zoneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/zones/" + zoneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateZoneInvalidLatitude() throws Exception {
        String token = registerUserAndGetToken("invalidlat@test.com", "invalidlatuser");

        MonitoringZoneRequest request = MonitoringZoneRequest.builder()
                .name("Invalid Zone")
                .description("Invalid latitude")
                .latitude(91.0)
                .longitude(0.0)
                .radius(1000.0)
                .build();

        mockMvc.perform(post("/api/zones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status_code").value(422));
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"latitude\":10,\"longitude\":20,\"radius\":100}"))
                .andExpect(status().isUnauthorized());
    }
}
