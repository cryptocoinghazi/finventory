package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.UserRepository;
import com.finventory.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DatabaseBackupIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtService jwtService;

    @Autowired private UserRepository userRepository;

    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin =
                User.builder()
                        .username("admin")
                        .email("admin@example.com")
                        .password("password")
                        .role(Role.ADMIN)
                        .build();
        userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        User user =
                User.builder()
                        .username("user")
                        .email("user@example.com")
                        .password("password")
                        .role(Role.USER)
                        .build();
        userRepository.save(user);
        userToken = "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void backupEndpoints_ShouldBeAdminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/migration/backups").header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAndDownloadBackup_ShouldWorkForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/migration/backups").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.fileName").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty());

        String listJson =
                mockMvc.perform(
                                get("/api/v1/admin/migration/backups")
                                        .header("Authorization", adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").isNotEmpty())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode parsed = objectMapper.readTree(listJson);
        String id = parsed.get(0).get("id").asText();

        mockMvc.perform(
                        get("/api/v1/admin/migration/backups/" + id + "/download")
                                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/sql"))
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                org.hamcrest.Matchers.containsString("attachment")));
    }
}
