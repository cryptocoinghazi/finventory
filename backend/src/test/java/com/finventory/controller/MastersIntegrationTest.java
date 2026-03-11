package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.CreateMigrationRunRequest;
import com.finventory.dto.AuthenticationRequest;
import com.finventory.dto.AuthenticationResponse;
import com.finventory.dto.ItemDto;
import com.finventory.dto.MigrationPipelinePreset;
import com.finventory.dto.MigrationPipelineProgressDto;
import com.finventory.dto.MigrationPipelineStartRequest;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.GLLineRepository;
import com.finventory.repository.GLTransactionRepository;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.PurchaseInvoiceRepository;
import com.finventory.repository.PurchaseReturnRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.SalesReturnRepository;
import com.finventory.repository.StockLedgerRepository;
import com.finventory.repository.UserRepository;
import com.finventory.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
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
    @Autowired private ItemRepository itemRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;
    @Autowired private PurchaseInvoiceRepository purchaseInvoiceRepository;
    @Autowired private PurchaseReturnRepository purchaseReturnRepository;
    @Autowired private StockLedgerRepository stockLedgerRepository;
    @Autowired private GLLineRepository glLineRepository;
    @Autowired private GLTransactionRepository glTransactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        stockLedgerRepository.deleteAll();
        glLineRepository.deleteAll();
        glTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesInvoiceRepository.deleteAll();
        purchaseReturnRepository.deleteAll();
        purchaseInvoiceRepository.deleteAll();
        itemRepository.deleteAll();
        partyRepository.deleteAll();
        warehouseRepository.deleteAll();
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
        mockMvc.perform(
                        post("/api/v1/items")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.code").value("TP-001"));

        // Get All Items
        mockMvc.perform(get("/api/v1/items").header("Authorization", jwtToken))
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

        mockMvc.perform(
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

    @Test
    void testMigrationDryRunFullSafePipeline() throws Exception {
        String dumpPath = Path.of("..", "docs", "nexo.sql").toAbsolutePath().normalize().toString();

        CreateMigrationRunRequest createRequest =
                CreateMigrationRunRequest.builder()
                        .sourceSystem("NEXOPOS")
                        .sourceReference(dumpPath)
                        .dryRun(true)
                        .build();

        String createResponse =
                mockMvc.perform(
                                post("/api/v1/admin/migration/runs")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID runId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        MigrationPipelineStartRequest startRequest =
                MigrationPipelineStartRequest.builder().preset(MigrationPipelinePreset.DRY_RUN_FULL_SAFE).build();

        mockMvc.perform(
                        post("/api/v1/admin/migration/runs/" + runId + "/pipeline/full-safe/start")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk());

        long deadline = System.currentTimeMillis() + 120_000;
        MigrationPipelineProgressDto progress = null;
        while (System.currentTimeMillis() < deadline) {
            String progressJson =
                    mockMvc.perform(
                                    get("/api/v1/admin/migration/runs/" + runId + "/pipeline/full-safe/progress")
                                            .header("Authorization", jwtToken)
                                            .param("preset", "DRY_RUN_FULL_SAFE"))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            progress = objectMapper.readValue(progressJson, MigrationPipelineProgressDto.class);
            if (!progress.isActive()) {
                break;
            }
            Thread.sleep(250);
        }

        Assertions.assertNotNull(progress);
        Assertions.assertFalse(progress.isActive(), "Pipeline did not finish before timeout");
        Assertions.assertEquals("Finished", progress.getSummary());
        Assertions.assertEquals(0L, progress.getErrorsCount(), "Pipeline errorsCount must be 0");
        Assertions.assertNotNull(progress.getRunStatus());
        Assertions.assertEquals("COMPLETED", progress.getRunStatus().name());
        Assertions.assertTrue(progress.getCompletedStages().contains("FINALIZE"));

        String stagesJson =
                mockMvc.perform(
                                get("/api/v1/admin/migration/runs/" + runId + "/stages")
                                        .header("Authorization", jwtToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<MigrationStageExecutionDto> stages =
                Arrays.asList(objectMapper.readValue(stagesJson, MigrationStageExecutionDto[].class));

        Map<String, Object> stageSummaries = new LinkedHashMap<>();
        for (MigrationStageExecutionDto e : stages) {
            if (e.getStageKey() == null || e.getStatsJson() == null || e.getStatsJson().isBlank()) {
                continue;
            }
            Map<String, Object> stats = objectMapper.readValue(e.getStatsJson(), Map.class);
            Map<String, Object> picked = new LinkedHashMap<>();
            for (String k :
                    List.of(
                            "implemented",
                            "message",
                            "dumpPath",
                            "insertStatements",
                            "found",
                            "inScope",
                            "created",
                            "updated",
                            "wouldCreate",
                            "wouldUpdate",
                            "fixedCustomersEnsured",
                            "vendorsCreatedFromCategories",
                            "vendorsWouldCreateFromCategories",
                            "itemsWouldCreate",
                            "itemsWouldUpdate",
                            "vendorMapped",
                            "vendorMissingMap",
                            "unsupportedDescriptionNonEmpty",
                            "unsupportedActivePresent",
                            "unsupportedCogsCandidatesPresent",
                            "stockAdjustmentsWouldCreate",
                            "paidInvoices",
                            "pendingInvoices",
                            "invoiceLinesWouldCreate",
                            "skippedMissingProductLines",
                            "warnings",
                            "errors")) {
                if (stats.containsKey(k)) {
                    picked.put(k, stats.get(k));
                }
            }
            stageSummaries.put(e.getStageKey(), picked);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", runId.toString());
        summary.put("runStatus", progress.getRunStatus() == null ? null : progress.getRunStatus().name());
        summary.put("warningsCount", progress.getWarningsCount());
        summary.put("errorsCount", progress.getErrorsCount());
        summary.put("pipelineSummary", progress.getSummary());
        summary.put("stages", stageSummaries);

        System.out.println(objectMapper.writeValueAsString(summary));
    }
}
