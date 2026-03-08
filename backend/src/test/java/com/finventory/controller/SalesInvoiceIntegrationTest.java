package com.finventory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.AuthenticationRequest;
import com.finventory.dto.AuthenticationResponse;
import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.model.Warehouse;
import com.finventory.repository.GLLineRepository;
import com.finventory.repository.GLTransactionRepository;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.StockLedgerRepository;
import com.finventory.repository.UserRepository;
import com.finventory.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class SalesInvoiceIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private GLLineRepository glLineRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private PartyRepository partyRepository;
  @Autowired private ItemRepository itemRepository;
  @Autowired private WarehouseRepository warehouseRepository;
  @Autowired private SalesInvoiceRepository salesInvoiceRepository;
  @Autowired private StockLedgerRepository stockLedgerRepository;
  @Autowired private GLTransactionRepository glTransactionRepository;

  private String jwtToken;
  private Party testParty;
  private Item testItem;
  private Warehouse testWarehouse;

  @BeforeEach
  void setUp() throws Exception {
    stockLedgerRepository.deleteAll();
    glTransactionRepository.deleteAll();
    salesInvoiceRepository.deleteAll();
    partyRepository.deleteAll();
    itemRepository.deleteAll();
    warehouseRepository.deleteAll();
    userRepository.deleteAll();

    // Create Admin User
    User admin = User.builder()
            .username("admin")
            .email("admin@finventory.com")
            .password(passwordEncoder.encode("admin123"))
            .role(Role.ADMIN)
            .build();
    userRepository.save(admin);

    // Login to get Token
    AuthenticationRequest loginRequest = AuthenticationRequest.builder()
            .username("admin")
            .password("admin123")
            .build();

    String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

    AuthenticationResponse authResponse = objectMapper.readValue(loginResponse, AuthenticationResponse.class);
    jwtToken = "Bearer " + authResponse.getToken();

    // Create Test Party
    testParty = Party.builder()
            .name("Test Customer")
            .type(Party.PartyType.CUSTOMER)
            .gstin("29ABCDE1234F1Z5")
            .build();
    testParty = partyRepository.save(testParty);

    // Create Test Item
    testItem = Item.builder()
            .name("Test Product")
            .code("TP-001")
            .taxRate(new BigDecimal("18.00"))
            .unitPrice(new BigDecimal("100.00"))
            .uom("PCS")
            .build();
    testItem = itemRepository.save(testItem);

    // Create Test Warehouse
    testWarehouse = Warehouse.builder()
            .name("Main Warehouse")
            .location("123 Industrial Area")
            .stateCode("29") // Karnataka
            .build();
    testWarehouse = warehouseRepository.save(testWarehouse);
  }

  @Test
  void testCreateSalesInvoice_IntraState() throws Exception {
    SalesInvoiceLineDto lineDto = SalesInvoiceLineDto.builder()
            .itemId(testItem.getId())
            .quantity(new BigDecimal("2"))
            .unitPrice(new BigDecimal("100.00"))
            .build();

    SalesInvoiceDto invoiceDto = SalesInvoiceDto.builder()
            .invoiceNumber("INV-001")
            .invoiceDate(LocalDate.now())
            .partyId(testParty.getId()) // Party GSTIN starts with 29 (from setup)
            .warehouseId(testWarehouse.getId()) // Warehouse state is 29
            .lines(List.of(lineDto))
            .build();

    mockMvc.perform(post("/api/v1/sales-invoices")
            .header("Authorization", jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invoiceDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
            .andExpect(jsonPath("$.totalTaxableAmount").value(200.00)) // 2 * 100
            .andExpect(jsonPath("$.totalTaxAmount").value(36.00)) // 200 * 18% = 36
            .andExpect(jsonPath("$.totalCgstAmount").value(18.00)) // 36 / 2
            .andExpect(jsonPath("$.totalSgstAmount").value(18.00)) // 36 / 2
            .andExpect(jsonPath("$.totalIgstAmount").value(0.00))
            .andExpect(jsonPath("$.grandTotal").value(236.00)); // 200 + 36

    // Verify Posting
    org.junit.jupiter.api.Assertions.assertEquals(1, stockLedgerRepository.count(), "Should create 1 stock ledger entry");
    org.junit.jupiter.api.Assertions.assertEquals(1, glTransactionRepository.count(), "Should create 1 GL transaction");
    // AR, Sales, CGST, SGST = 4 lines
    org.junit.jupiter.api.Assertions.assertEquals(4, glLineRepository.count(), "Should create 4 GL lines (AR, Sales, CGST, SGST)");
  }

  @Test
  void testCreateSalesInvoice_InterState() throws Exception {
    // Create Party in different state (e.g., 27 - Maharashtra)
    Party interStateParty = Party.builder()
            .name("Inter State Customer")
            .type(Party.PartyType.CUSTOMER)
            .gstin("27ABCDE1234F1Z5")
            .build();
    interStateParty = partyRepository.save(interStateParty);

    SalesInvoiceLineDto lineDto = SalesInvoiceLineDto.builder()
            .itemId(testItem.getId())
            .quantity(new BigDecimal("2"))
            .unitPrice(new BigDecimal("100.00"))
            .build();

    SalesInvoiceDto invoiceDto = SalesInvoiceDto.builder()
            .invoiceNumber("INV-002")
            .invoiceDate(LocalDate.now())
            .partyId(interStateParty.getId())
            .warehouseId(testWarehouse.getId()) // Warehouse state is 29
            .lines(List.of(lineDto))
            .build();

    mockMvc.perform(post("/api/v1/sales-invoices")
            .header("Authorization", jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invoiceDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-002"))
            .andExpect(jsonPath("$.totalTaxableAmount").value(200.00))
            .andExpect(jsonPath("$.totalTaxAmount").value(36.00))
            .andExpect(jsonPath("$.totalCgstAmount").value(0.00))
            .andExpect(jsonPath("$.totalSgstAmount").value(0.00))
            .andExpect(jsonPath("$.totalIgstAmount").value(36.00)) // Full tax as IGST
            .andExpect(jsonPath("$.grandTotal").value(236.00));

    // Verify Posting for Inter-state
    // AR, Sales, IGST = 3 lines
    // Note: glLineRepository count will include lines from previous test if not cleared.
    // But setUp clears repositories. So this count is for this test only.
    org.junit.jupiter.api.Assertions.assertEquals(1, glTransactionRepository.count(), "Should create 1 GL transaction");
    org.junit.jupiter.api.Assertions.assertEquals(3, glLineRepository.count(), "Should create 3 GL lines (AR, Sales, IGST)");
  }

  @Test
  void testCreateSalesInvoiceValidationFailure() throws Exception {
    SalesInvoiceDto invalidDto = SalesInvoiceDto.builder()
            .invoiceDate(LocalDate.now())
            // Missing Party ID
            // Missing Lines
            .build();

    mockMvc.perform(post("/api/v1/sales-invoices")
            .header("Authorization", jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
  }
}
