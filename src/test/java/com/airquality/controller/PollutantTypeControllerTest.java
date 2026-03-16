package com.airquality.controller;

import com.airquality.dto.request.PollutantTypeRequest;
import com.airquality.dto.request.PollutantTypeUpdateRequest;
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
public class PollutantTypeControllerTest {

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

    @Test
    public void testCreatePollutant() throws Exception {
        String token = registerUserAndGetToken("pollutant@test.com", "polluser", "Pollutant User");

        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("PM2.5")
                .description("Fine particulate matter")
                .unit("ug/m3")
                .safeThreshold(12.0)
                .warningThreshold(35.4)
                .dangerThreshold(55.4)
                .build();

        mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PM2.5"))
                .andExpect(jsonPath("$.unit").value("ug/m3"))
                .andExpect(jsonPath("$.safe_threshold").value(12.0))
                .andExpect(jsonPath("$.warning_threshold").value(35.4))
                .andExpect(jsonPath("$.danger_threshold").value(55.4));
    }

    @Test
    public void testGetAllPollutants() throws Exception {
        String token = registerUserAndGetToken("getall@test.com", "getalluser", "GetAll User");

        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("CO")
                .description("Carbon monoxide")
                .unit("ppm")
                .safeThreshold(4.4)
                .warningThreshold(9.4)
                .dangerThreshold(12.4)
                .build();

        mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pollutants")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("CO"));
    }

    @Test
    public void testGetPollutantById() throws Exception {
        String token = registerUserAndGetToken("getbyid@test.com", "getbyiduser", "GetById User");

        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("NO2")
                .description("Nitrogen dioxide")
                .unit("ppb")
                .safeThreshold(53.0)
                .warningThreshold(100.0)
                .dangerThreshold(360.0)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long pollutantId = created.get("id").asLong();

        mockMvc.perform(get("/api/pollutants/" + pollutantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pollutantId))
                .andExpect(jsonPath("$.name").value("NO2"));
    }

    @Test
    public void testUpdatePollutant() throws Exception {
        String token = registerUserAndGetToken("updatepoll@test.com", "updatepoll", "Update Pollutant");

        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("SO2")
                .description("Sulfur dioxide")
                .unit("ppb")
                .safeThreshold(35.0)
                .warningThreshold(75.0)
                .dangerThreshold(185.0)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long pollutantId = created.get("id").asLong();

        PollutantTypeUpdateRequest updateRequest = PollutantTypeUpdateRequest.builder()
                .description("Updated sulfur dioxide description")
                .safeThreshold(40.0)
                .build();

        mockMvc.perform(put("/api/pollutants/" + pollutantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated sulfur dioxide description"))
                .andExpect(jsonPath("$.safe_threshold").value(40.0));
    }

    @Test
    public void testDeletePollutant() throws Exception {
        String token = registerUserAndGetToken("delpoll@test.com", "delpoll", "Delete Pollutant");

        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("O3")
                .description("Ozone")
                .unit("ppb")
                .safeThreshold(54.0)
                .warningThreshold(70.0)
                .dangerThreshold(85.0)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long pollutantId = created.get("id").asLong();

        mockMvc.perform(delete("/api/pollutants/" + pollutantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pollutants/" + pollutantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreatePollutantInvalidThresholds() throws Exception {
        String token = registerUserAndGetToken("invalid@test.com", "invaliduser", "Invalid User");

        // safe > warning should fail
        PollutantTypeRequest request = PollutantTypeRequest.builder()
                .name("BadPollutant")
                .description("Invalid thresholds")
                .unit("ppm")
                .safeThreshold(100.0)
                .warningThreshold(50.0)
                .dangerThreshold(200.0)
                .build();

        mockMvc.perform(post("/api/pollutants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }
}
