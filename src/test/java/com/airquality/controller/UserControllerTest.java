package com.airquality.controller;

import com.airquality.dto.request.RegisterRequest;
import com.airquality.dto.request.UserUpdateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

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

    private Long registerUserAndGetId(String email, String username, String fullName) throws Exception {
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
        return json.get("user").get("id").asLong();
    }

    @Test
    public void testGetAllUsers() throws Exception {
        String token = registerUserAndGetToken("user1@test.com", "userone", "User One");
        registerUserAndGetToken("user2@test.com", "usertwo", "User Two");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[0].username").isNotEmpty());
    }

    @Test
    public void testGetUserById() throws Exception {
        String token = registerUserAndGetToken("findme@test.com", "findmeuser", "Find Me");

        // Get the user id from the /api/auth/me endpoint
        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode meJson = objectMapper.readTree(meResult.getResponse().getContentAsString());
        Long userId = meJson.get("id").asLong();

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("findme@test.com"))
                .andExpect(jsonPath("$.username").value("findmeuser"))
                .andExpect(jsonPath("$.full_name").value("Find Me"));
    }

    @Test
    public void testGetNonExistentUser() throws Exception {
        String token = registerUserAndGetToken("exists@test.com", "existsuser", "Exists User");

        mockMvc.perform(get("/api/users/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateOwnUserProfile() throws Exception {
        String token = registerUserAndGetToken("update@test.com", "updateuser", "Update User");

        // Get user id
        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode meJson = objectMapper.readTree(meResult.getResponse().getContentAsString());
        Long userId = meJson.get("id").asLong();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .username("updateduser")
                .build();

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Updated Name"))
                .andExpect(jsonPath("$.username").value("updateduser"));
    }

    @Test
    public void testGetUsersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
