package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.AuthenticationRequest;
import com.finventory.dto.AuthenticationResponse;
import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.dto.PurchaseInvoiceLineDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.model.Warehouse;
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
import java.time.LocalDate;
import java.util.List;
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
class PurchaseInvoiceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PartyRepository partyRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private PurchaseInvoiceRepository purchaseInvoiceRepository;
    @Autowired private PurchaseReturnRepository purchaseReturnRepository;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;
    @Autowired private StockLedgerRepository stockLedgerRepository;
    @Autowired private GLTransactionRepository glTransactionRepository;
    @Autowired private GLLineRepository glLineRepository;

    private String jwtToken;
    private Party testParty;
    private Item testItem;
    private Warehouse testWarehouse;

    @BeforeEach
    void setUp() throws Exception {
        documentSequenceRepository.deleteAll();
        stockLedgerRepository.deleteAll();
        glLineRepository.deleteAll();
        glTransactionRepository.deleteAll();
        purchaseReturnRepository.deleteAll();
        purchaseInvoiceRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesInvoiceRepository.deleteAll();
        partyRepository.deleteAll();
        itemRepository.deleteAll();
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

        // Create Test Party (Vendor)
        testParty =
                Party.builder()
                        .name("Test Vendor")
                        .type(Party.PartyType.VENDOR)
                        .gstin("29ABCDE1234F1Z5") // Karnataka
                        .stateCode("29")
                        .build();
        partyRepository.save(testParty);

        // Create Test Warehouse
        testWarehouse =
                Warehouse.builder()
                        .name("Main Warehouse")
                        .stateCode("29") // Karnataka (Intra-state)
                        .build();
        warehouseRepository.save(testWarehouse);

        // Create Test Item
        testItem =
                Item.builder()
                        .name("Test Product")
                        .code("SKU-001")
                        .unitPrice(new BigDecimal("100.00"))
                        .uom("PCS")
                        .taxRate(new BigDecimal("18.00"))
                        .build();
        itemRepository.save(testItem);
    }

    @Test
    void shouldGenerateInvoiceNumberWhenNull() throws Exception {
        PurchaseInvoiceDto request =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now())
                        .vendorInvoiceNumber("VEND-001")
                        .partyId(testParty.getId())
                        .warehouseId(testWarehouse.getId())
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(testItem.getId())
                                                .quantity(new BigDecimal("10"))
                                                .unitPrice(new BigDecimal("100"))
                                                .build()))
                        .build();

        mockMvc.perform(
                        post("/api/v1/purchase-invoices")
                                .header("Authorization", "Bearer " + jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty())
                .andExpect(
                        jsonPath("$.invoiceNumber")
                                .value(org.hamcrest.Matchers.startsWith("PINV/")));
    }

    @Test
    void createPurchaseInvoice_ShouldSucceed() throws Exception {
        PurchaseInvoiceLineDto lineDto =
                PurchaseInvoiceLineDto.builder()
                        .itemId(testItem.getId())
                        .quantity(new BigDecimal("10"))
                        .unitPrice(new BigDecimal("50.00"))
                        .taxRate(new BigDecimal("18.00"))
                        .build();

        PurchaseInvoiceDto invoiceDto =
                PurchaseInvoiceDto.builder()
                        .invoiceNumber("PINV-001")
                        .vendorInvoiceNumber("VINV-999")
                        .invoiceDate(LocalDate.now())
                        .partyId(testParty.getId())
                        .warehouseId(testWarehouse.getId())
                        .lines(List.of(lineDto))
                        .build();

        mockMvc.perform(
                        post("/api/purchase-invoices")
                                .header("Authorization", jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invoiceDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grandTotal").value(590.0)); // 500 + 18% tax = 590

        // Verify Stock Ledger (Should be IN)
        Assertions.assertEquals(1, stockLedgerRepository.count());
        Assertions.assertEquals(
                0,
                stockLedgerRepository.findAll().get(0).getQtyIn().compareTo(new BigDecimal("10")));

        // Verify GL Transaction
        Assertions.assertEquals(1, glTransactionRepository.count());
        Assertions.assertEquals(
                4, glLineRepository.count(), "Should create 4 GL lines (AP, Purchase, CGST, SGST)");
    }
}
