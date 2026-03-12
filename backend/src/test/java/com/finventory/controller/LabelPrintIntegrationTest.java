package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.AuthenticationRequest;
import com.finventory.dto.AuthenticationResponse;
import com.finventory.dto.ItemDto;
import com.finventory.model.LabelPrintJobStatus;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.LabelPrintJobRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class LabelPrintIntegrationTest {
    private static final int ITEM_CODE_SUFFIX_LENGTH = 8;
    private static final String ITEM_CODE_PREFIX = "LBL-";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private LabelPrintJobRepository labelPrintJobRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String jwtToken;

    private String newItemCode() {
        return ITEM_CODE_PREFIX
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, ITEM_CODE_SUFFIX_LENGTH);
    }

    @BeforeEach
    void setUp() throws Exception {
        labelPrintJobRepository.deleteAll();
        itemRepository.deleteAll();
        partyRepository.deleteAll();
        userRepository.deleteAll();

        User admin =
                User.builder()
                        .username("admin")
                        .email("admin@finventory.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build();
        userRepository.save(admin);

        AuthenticationRequest loginRequest =
                AuthenticationRequest.builder().username("admin").password("admin123").build();

        String loginResponse =
                mockMvc.perform(
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
    void prepare_AllValid_CreatesPreparedJob() throws Exception {
        ItemDto itemDto =
                ItemDto.builder()
                        .name("Test Product")
                        .code(newItemCode())
                        .barcode("5901234123457")
                        .hsnCode("1234")
                        .taxRate(new BigDecimal("18.00"))
                        .unitPrice(new BigDecimal("100.00"))
                        .uom("PCS")
                        .build();

        String itemResponse =
                mockMvc.perform(
                                post("/api/v1/items")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(itemDto)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ItemDto created = objectMapper.readValue(itemResponse, ItemDto.class);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(Map.of("itemId", created.getId(), "quantity", 2)));
        body.put("templateName", "LABEL_2X1");
        body.put("barcodeFormat", "AUTO");
        body.put("includeItemCode", false);

        mockMvc.perform(
                        post("/api/v1/labels/print-jobs/prepare")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARED"))
                .andExpect(jsonPath("$.totalLabelsRequested").value(2))
                .andExpect(jsonPath("$.totalLabelsValid").value(2))
                .andExpect(jsonPath("$.items[0].itemId").value(created.getId().toString()))
                .andExpect(jsonPath("$.items[0].effectiveBarcodeFormat").value("EAN13"))
                .andExpect(jsonPath("$.invalidItems").isArray());
    }

    @Test
    void prepare_InvalidEan13_FailsValidationAndReturnsInvalidItems() throws Exception {
        ItemDto itemDto =
                ItemDto.builder()
                        .name("Bad Barcode Product")
                        .code(newItemCode())
                        .barcode("1234567890123")
                        .hsnCode("1234")
                        .taxRate(new BigDecimal("18.00"))
                        .unitPrice(new BigDecimal("100.00"))
                        .uom("PCS")
                        .build();

        String itemResponse =
                mockMvc.perform(
                                post("/api/v1/items")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(itemDto)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ItemDto created = objectMapper.readValue(itemResponse, ItemDto.class);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(Map.of("itemId", created.getId(), "quantity", 1)));
        body.put("templateName", "LABEL_2X1");
        body.put("barcodeFormat", "EAN13");
        body.put("includeItemCode", false);

        mockMvc.perform(
                        post("/api/v1/labels/print-jobs/prepare")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED_VALIDATION"))
                .andExpect(jsonPath("$.totalLabelsValid").value(0))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.invalidItems[0].itemId").value(created.getId().toString()))
                .andExpect(jsonPath("$.invalidItems[0].errors[0]").exists());
    }

    @Test
    void updateStatus_UpdatesJobStatus() throws Exception {
        ItemDto itemDto =
                ItemDto.builder()
                        .name("Test Product")
                        .code(newItemCode())
                        .barcode("5901234123457")
                        .hsnCode("1234")
                        .taxRate(new BigDecimal("18.00"))
                        .unitPrice(new BigDecimal("100.00"))
                        .uom("PCS")
                        .build();

        String itemResponse =
                mockMvc.perform(
                                post("/api/v1/items")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(itemDto)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ItemDto created = objectMapper.readValue(itemResponse, ItemDto.class);

        Map<String, Object> prepareBody = new LinkedHashMap<>();
        prepareBody.put("items", List.of(Map.of("itemId", created.getId(), "quantity", 1)));
        prepareBody.put("templateName", "LABEL_2X1");
        prepareBody.put("barcodeFormat", "AUTO");
        prepareBody.put("includeItemCode", false);

        String prepareResponse =
                mockMvc.perform(
                                post("/api/v1/labels/print-jobs/prepare")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(prepareBody)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(prepareResponse).get("jobId").asText());

        Map<String, Object> statusBody = Map.of("status", "PRINTED");

        mockMvc.perform(
                        patch("/api/v1/labels/print-jobs/{id}/status", jobId)
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusBody)))
                .andExpect(status().isOk());

        var job = labelPrintJobRepository.findById(jobId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(LabelPrintJobStatus.PRINTED, job.getStatus());
    }
}
