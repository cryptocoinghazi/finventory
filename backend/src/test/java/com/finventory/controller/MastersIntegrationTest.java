package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.AuthenticationRequest;
import com.finventory.dto.AuthenticationResponse;
import com.finventory.dto.ItemDto;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MastersIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String jwtToken;

  @BeforeEach
  void setUp() throws Exception {
    userRepository.deleteAll();

    // Create Admin User
    User admin =
        User.builder()
            .username("admin")
            .email("admin@finventory.com")
            .password(passwordEncoder.encode("admin123"))
            .role(Role.ADMIN)
            .build();
    userRepository.save(admin);

    // Login to get Token
    AuthenticationRequest loginRequest =
        AuthenticationRequest.builder().username("admin").password("admin123").build();

    String loginResponse =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    AuthenticationResponse authResponse =
        objectMapper.readValue(loginResponse, AuthenticationResponse.class);
    jwtToken = "Bearer " + authResponse.getToken();
  }

  @Test
  void testCreateAndGetItem() throws Exception {
    ItemDto itemDto =
        ItemDto.builder()
            .name("Test Product")
            .code("TP-001")
            .hsnCode("1234")
            .taxRate(new BigDecimal("18.00"))
            .unitPrice(new BigDecimal("100.00"))
            .uom("PCS")
            .build();

    // Create Item
    mockMvc
        .perform(
            post("/api/v1/items")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Product"))
        .andExpect(jsonPath("$.code").value("TP-001"));

    // Get All Items
    mockMvc
        .perform(get("/api/v1/items").header("Authorization", jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("TP-001"));
  }

  @Test
  void testCreateItemValidationFailure() throws Exception {
    ItemDto invalidItem =
        ItemDto.builder()
            .name("") // Invalid: Blank
            .code("") // Invalid: Blank
            .taxRate(new BigDecimal("-5.00")) // Invalid: Negative
            .unitPrice(new BigDecimal("-100.00")) // Invalid: Negative
            .uom("") // Invalid: Blank
            .build();

    mockMvc
        .perform(
            post("/api/v1/items")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidItem)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.details.name").exists())
        .andExpect(jsonPath("$.details.code").exists())
        .andExpect(jsonPath("$.details.taxRate").exists())
        .andExpect(jsonPath("$.details.unitPrice").exists())
        .andExpect(jsonPath("$.details.uom").exists());
  }
}
