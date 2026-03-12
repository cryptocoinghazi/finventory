package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.AuthenticationRequest;
import com.finventory.dto.AuthenticationResponse;
import com.finventory.dto.CreateMigrationRunRequest;
import com.finventory.dto.ItemDto;
import com.finventory.dto.MigrationPipelinePreset;
import com.finventory.dto.MigrationPipelineProgressDto;
import com.finventory.dto.MigrationPipelineStartRequest;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.dto.PartyDto;
import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.dto.PurchaseInvoiceLineDto;
import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.model.MigrationRun;
import com.finventory.model.MigrationRunStatus;
import com.finventory.model.Party;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.GLLineRepository;
import com.finventory.repository.GLTransactionRepository;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.MigrationIdMapRepository;
import com.finventory.repository.MigrationLogEntryRepository;
import com.finventory.repository.MigrationRunRepository;
import com.finventory.repository.MigrationStageExecutionRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.PurchaseInvoiceRepository;
import com.finventory.repository.PurchaseReturnRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.SalesReturnRepository;
import com.finventory.repository.StockLedgerRepository;
import com.finventory.repository.UserRepository;
import com.finventory.repository.WarehouseRepository;
import com.finventory.service.NexoDumpSqlService;
import com.finventory.service.NexoMigrationItemsStagesService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
    @Autowired private MigrationRunRepository migrationRunRepository;
    @Autowired private MigrationStageExecutionRepository migrationStageExecutionRepository;
    @Autowired private MigrationLogEntryRepository migrationLogEntryRepository;
    @Autowired private MigrationIdMapRepository migrationIdMapRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NexoMigrationItemsStagesService itemsStagesService;
    @Autowired private NexoDumpSqlService dumpSql;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        migrationStageExecutionRepository.deleteAll();
        migrationLogEntryRepository.deleteAll();
        migrationIdMapRepository.deleteAll();
        migrationRunRepository.deleteAll();
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
    void deleteParty_ShouldReturnConflictWhenPartyIsUsed() throws Exception {
        String partyJson =
                mockMvc.perform(
                                post("/api/v1/parties")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        PartyDto.builder()
                                                                .name("Delete Blocked Customer")
                                                                .type(Party.PartyType.CUSTOMER)
                                                                .gstin("29DELPCUST0000C1Z5")
                                                                .stateCode("29")
                                                                .phone("9000000000")
                                                                .build())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        PartyDto party = objectMapper.readValue(partyJson, PartyDto.class);

        String warehouseJson =
                mockMvc.perform(
                                post("/api/v1/warehouses")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"Delete Blocked WH","stateCode":"29","location":"Test"}
                                                """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID warehouseId = UUID.fromString(objectMapper.readTree(warehouseJson).get("id").asText());

        String itemJson =
                mockMvc.perform(
                                post("/api/v1/items")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        ItemDto.builder()
                                                                .name("Delete Blocked Item")
                                                                .code("DEL-BLOCK-001")
                                                                .taxRate(new BigDecimal("18.00"))
                                                                .unitPrice(new BigDecimal("100.00"))
                                                                .uom("PCS")
                                                                .build())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID itemId = UUID.fromString(objectMapper.readTree(itemJson).get("id").asText());

        SalesInvoiceDto invoice =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now())
                        .partyId(party.getId())
                        .warehouseId(warehouseId)
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(itemId)
                                                .quantity(new BigDecimal("1"))
                                                .unitPrice(new BigDecimal("100.00"))
                                                .build()))
                        .build();

        mockMvc.perform(
                        post("/api/v1/sales-invoices")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invoice)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        delete("/api/v1/parties/{id}", party.getId())
                                .header("Authorization", jwtToken))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Cannot delete party because it is used in invoices or ledger entries"));
    }

    @Test
    void deleteParty_ShouldForceDeleteWhenForceIsTrue() throws Exception {
        String partyJson =
                mockMvc.perform(
                                post("/api/v1/parties")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        PartyDto.builder()
                                                                .name("Force Delete Vendor")
                                                                .type(Party.PartyType.VENDOR)
                                                                .gstin("29FORCEVEND0000C1Z5")
                                                                .stateCode("29")
                                                                .phone("9000000001")
                                                                .build())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        PartyDto party = objectMapper.readValue(partyJson, PartyDto.class);

        String warehouseJson =
                mockMvc.perform(
                                post("/api/v1/warehouses")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"Force Delete WH","stateCode":"29","location":"Test"}
                                                """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID warehouseId = UUID.fromString(objectMapper.readTree(warehouseJson).get("id").asText());

        String itemJson =
                mockMvc.perform(
                                post("/api/v1/items")
                                        .header("Authorization", jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        ItemDto.builder()
                                                                .name("Force Delete Item")
                                                                .code("FORCE-DEL-001")
                                                                .taxRate(new BigDecimal("18.00"))
                                                                .unitPrice(new BigDecimal("100.00"))
                                                                .uom("PCS")
                                                                .vendorId(party.getId())
                                                                .build())))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        UUID itemId = UUID.fromString(objectMapper.readTree(itemJson).get("id").asText());

        PurchaseInvoiceDto invoice =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now())
                        .partyId(party.getId())
                        .warehouseId(warehouseId)
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(itemId)
                                                .quantity(new BigDecimal("1"))
                                                .unitPrice(new BigDecimal("100.00"))
                                                .build()))
                        .build();

        mockMvc.perform(
                        post("/api/v1/purchase-invoices")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invoice)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        delete("/api/v1/parties/{id}", party.getId())
                                .param("force", "true")
                                .header("Authorization", jwtToken))
                .andExpect(status().isNoContent());

        Assertions.assertFalse(partyRepository.existsById(party.getId()));
        Assertions.assertTrue(purchaseInvoiceRepository.findAll().isEmpty());
        Assertions.assertTrue(purchaseReturnRepository.findAll().isEmpty());
        Assertions.assertTrue(glTransactionRepository.findAll().isEmpty());

        com.finventory.model.Item persistedItem = itemRepository.findById(itemId).orElseThrow();
        Assertions.assertNull(persistedItem.getVendor());
    }

    @Test
    void testImportItems_UsesUnitQuantitiesForMissingPriceAndCogs() throws Exception {
        Path dumpPath = Path.of("..", "docs", "nexo.sql").toAbsolutePath().normalize();

        Map<Long, Pricing> unitQuantitiesPricing = loadUnitQuantitiesPricing(dumpPath);
        Assertions.assertFalse(unitQuantitiesPricing.isEmpty());

        HashSet<Long> soldPriceProductIds =
                loadSoldProductIdsWithPositiveValue(dumpPath, "unit_price");
        HashSet<Long> soldCogsProductIds =
                loadSoldProductIdsWithPositiveValue(dumpPath, "total_purchase_price");
        HashSet<Long> procurementProductIds =
                loadProcurementProductIdsWithPositivePurchasePrice(dumpPath);

        Optional<Long> candidate =
                unitQuantitiesPricing.entrySet().stream()
                        .filter(
                                e ->
                                        e.getValue().salePrice.compareTo(BigDecimal.ZERO) > 0
                                                && e.getValue().cogs.compareTo(BigDecimal.ZERO) > 0)
                        .map(Map.Entry::getKey)
                        .filter(id -> !soldPriceProductIds.contains(id))
                        .filter(id -> !soldCogsProductIds.contains(id))
                        .filter(id -> !procurementProductIds.contains(id))
                        .findFirst();

        if (candidate.isEmpty()) {
            candidate =
                    unitQuantitiesPricing.entrySet().stream()
                            .filter(e -> e.getValue().salePrice.compareTo(BigDecimal.ZERO) > 0)
                            .map(Map.Entry::getKey)
                            .filter(id -> !soldPriceProductIds.contains(id))
                            .findFirst();
        }

        Assertions.assertTrue(candidate.isPresent());
        long productId = candidate.get();
        Pricing expected = unitQuantitiesPricing.get(productId);

        MigrationRun run =
                migrationRunRepository.save(
                        MigrationRun.builder()
                                .sourceSystem("NEXOPOS")
                                .sourceReference(dumpPath.toString())
                                .dryRun(false)
                                .status(MigrationRunStatus.CREATED)
                                .startedAt(OffsetDateTime.now())
                                .scopeSourceIdMin(productId)
                                .scopeSourceIdMax(productId)
                                .build());

        itemsStagesService.importItems(run, dumpPath);

        com.finventory.model.MigrationIdMap map =
                migrationIdMapRepository
                        .findBySourceSystemAndEntityTypeAndSourceId("NEXOPOS", "ITEM", productId)
                        .orElseThrow();

        com.finventory.model.Item item = itemRepository.findById(map.getTargetId()).orElseThrow();
        Assertions.assertTrue(item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0);
        Assertions.assertEquals(0, item.getUnitPrice().compareTo(expected.salePrice));
        Assertions.assertTrue(item.getCogs().compareTo(BigDecimal.ZERO) >= 0);
        if (expected.cogs.compareTo(BigDecimal.ZERO) > 0
                && !soldCogsProductIds.contains(productId)
                && !procurementProductIds.contains(productId)) {
            Assertions.assertEquals(0, item.getCogs().compareTo(expected.cogs));
        }
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
                MigrationPipelineStartRequest.builder()
                        .preset(MigrationPipelinePreset.DRY_RUN_FULL_SAFE)
                        .build();

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
                                    get("/api/v1/admin/migration/runs/"
                                                    + runId
                                                    + "/pipeline/full-safe/progress")
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
                Arrays.asList(
                        objectMapper.readValue(stagesJson, MigrationStageExecutionDto[].class));

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
        summary.put(
                "runStatus",
                progress.getRunStatus() == null ? null : progress.getRunStatus().name());
        summary.put("warningsCount", progress.getWarningsCount());
        summary.put("errorsCount", progress.getErrorsCount());
        summary.put("pipelineSummary", progress.getSummary());
        summary.put("stages", stageSummaries);

        System.out.println(objectMapper.writeValueAsString(summary));
    }

    private Map<Long, Pricing> loadUnitQuantitiesPricing(Path dumpPath) throws Exception {
        Map<Long, Pricing> bestByProductId = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                "ns_nexopos_products_unit_quantities",
                (columns, values) -> {
                    Long productId = asLong(dumpSql.getByColumn(columns, values, "product_id"));
                    if (productId == null) {
                        return;
                    }

                    String type = dumpSql.getByColumn(columns, values, "type");
                    if (type != null && !type.equalsIgnoreCase("product")) {
                        return;
                    }

                    Long visible = asLong(dumpSql.getByColumn(columns, values, "visible"));
                    if (visible != null && visible != 1L) {
                        return;
                    }

                    boolean baseUnit =
                            dumpSql.getByColumn(columns, values, "convert_unit_id") == null;
                    BigDecimal salePrice =
                            asBigDecimal(dumpSql.getByColumn(columns, values, "sale_price"));
                    BigDecimal cogs = asBigDecimal(dumpSql.getByColumn(columns, values, "cogs"));
                    Pricing candidate = new Pricing(salePrice, cogs, baseUnit);

                    Pricing existing = bestByProductId.get(productId);
                    if (existing == null || isBetter(candidate, existing)) {
                        bestByProductId.put(productId, candidate);
                    }
                });
        return bestByProductId;
    }

    private HashSet<Long> loadSoldProductIdsWithPositiveValue(Path dumpPath, String column)
            throws Exception {
        HashSet<Long> ids = new HashSet<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                "ns_nexopos_orders_products",
                (columns, values) -> {
                    Long productId = asLong(dumpSql.getByColumn(columns, values, "product_id"));
                    if (productId == null) {
                        return;
                    }
                    String status = dumpSql.getByColumn(columns, values, "status");
                    if (status != null && !status.equalsIgnoreCase("sold")) {
                        return;
                    }
                    BigDecimal v = asBigDecimal(dumpSql.getByColumn(columns, values, column));
                    if (v.compareTo(BigDecimal.ZERO) > 0) {
                        ids.add(productId);
                    }
                });
        return ids;
    }

    private HashSet<Long> loadProcurementProductIdsWithPositivePurchasePrice(Path dumpPath)
            throws Exception {
        HashSet<Long> ids = new HashSet<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                "ns_nexopos_procurements_products",
                (columns, values) -> {
                    Long productId = asLong(dumpSql.getByColumn(columns, values, "product_id"));
                    if (productId == null) {
                        return;
                    }
                    BigDecimal v =
                            asBigDecimal(dumpSql.getByColumn(columns, values, "purchase_price"));
                    if (v.compareTo(BigDecimal.ZERO) > 0) {
                        ids.add(productId);
                    }
                });
        return ids;
    }

    private Long asLong(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return Long.parseLong(token.trim());
    }

    private BigDecimal asBigDecimal(String token) {
        if (token == null || token.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(token.trim());
    }

    private boolean isBetter(Pricing candidate, Pricing existing) {
        if (candidate.baseUnit && !existing.baseUnit) {
            return true;
        }
        if (!candidate.baseUnit && existing.baseUnit) {
            return false;
        }
        int saleCompare = candidate.salePrice.compareTo(existing.salePrice);
        if (saleCompare != 0) {
            return saleCompare > 0;
        }
        return candidate.cogs.compareTo(existing.cogs) > 0;
    }

    private record Pricing(BigDecimal salePrice, BigDecimal cogs, boolean baseUnit) {}
}
