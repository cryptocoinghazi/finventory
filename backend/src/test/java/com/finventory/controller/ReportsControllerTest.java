package com.finventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.StockSummaryDto;
import com.finventory.model.GLLine;
import com.finventory.model.GLTransaction;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.Role;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.User;
import com.finventory.model.Warehouse;
import com.finventory.repository.GLLineRepository;
import com.finventory.repository.GLTransactionRepository;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.StockLedgerRepository;
import com.finventory.repository.UserRepository;
import com.finventory.repository.WarehouseRepository;
import com.finventory.security.JwtService;
import com.finventory.service.ReportsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReportsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private StockLedgerRepository stockLedgerRepository;

  @Autowired
  private GLTransactionRepository glTransactionRepository;

  @Autowired
  private GLLineRepository glLineRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private WarehouseRepository warehouseRepository;

  @Autowired
  private PartyRepository partyRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private com.finventory.repository.SalesInvoiceRepository salesInvoiceRepository;

  @Autowired
  private com.finventory.repository.PurchaseInvoiceRepository purchaseInvoiceRepository;

  private String jwtToken;
  private Item testItem;
  private Warehouse testWarehouse;
  private Party testParty;

  @BeforeEach
  void setUp() {
    salesInvoiceRepository.deleteAll();
    purchaseInvoiceRepository.deleteAll();
    stockLedgerRepository.deleteAll();
    glLineRepository.deleteAll();
    glTransactionRepository.deleteAll();
    itemRepository.deleteAll();
    warehouseRepository.deleteAll();
    partyRepository.deleteAll();
    userRepository.deleteAll();

    User user = User.builder()
        .username("testuser")
        .email("testuser@example.com")
        .password("password")
        .role(Role.USER)
        .build();
    userRepository.save(user);
    jwtToken = "Bearer " + jwtService.generateToken(user);

    testItem = itemRepository.save(Item.builder()
        .name("Test Item")
        .code("ITEM-001")
        .unitPrice(new BigDecimal("100.00"))
        .taxRate(new BigDecimal("18.00"))
        .uom("PCS")
        .build());

    testWarehouse = warehouseRepository.save(Warehouse.builder()
        .name("Main Warehouse")
        .location("Mumbai")
        .stateCode("27")
        .build());

    testParty = partyRepository.save(Party.builder()
        .name("Test Customer")
        .type(Party.PartyType.CUSTOMER)
        .email("customer@test.com")
        .phone("1234567890")
        .stateCode("27")
        .build());
  }

  @Test
  void getGstr1_ShouldReturnCorrectData() throws Exception {
    // 1. Setup Sales Invoice
    com.finventory.model.SalesInvoice invoice = com.finventory.model.SalesInvoice.builder()
        .invoiceNumber("INV-GSTR-001")
        .invoiceDate(LocalDate.now())
        .party(testParty)
        .warehouse(testWarehouse)
        .totalTaxableAmount(new BigDecimal("100.00"))
        .totalTaxAmount(new BigDecimal("18.00"))
        .totalCgstAmount(new BigDecimal("9.00"))
        .totalSgstAmount(new BigDecimal("9.00"))
        .totalIgstAmount(BigDecimal.ZERO)
        .grandTotal(new BigDecimal("118.00"))
        .build();
    salesInvoiceRepository.save(invoice);

    // 2. Call API
    mockMvc.perform(get("/api/reports/gstr-1")
        .header("Authorization", jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].invoiceNumber").value("INV-GSTR-001"))
        .andExpect(jsonPath("$[0].totalAmount").value(118.0));
  }

  @Test
  void getStockSummary_ShouldReturnCorrectStock() throws Exception {
    // 1. Setup Stock Data (IN and OUT)
    StockLedgerEntry entryIn = StockLedgerEntry.builder()
        .date(LocalDate.now())
        .item(testItem)
        .warehouse(testWarehouse)
        .qtyIn(new BigDecimal("10"))
        .qtyOut(BigDecimal.ZERO)
        .refType(StockLedgerEntry.ReferenceType.PURCHASE_INVOICE)
        .refId(UUID.randomUUID())
        .build();
    stockLedgerRepository.save(entryIn);

    StockLedgerEntry entryOut = StockLedgerEntry.builder()
        .date(LocalDate.now())
        .item(testItem)
        .warehouse(testWarehouse)
        .qtyIn(BigDecimal.ZERO)
        .qtyOut(new BigDecimal("3"))
        .refType(StockLedgerEntry.ReferenceType.SALES_INVOICE)
        .refId(UUID.randomUUID())
        .build();
    stockLedgerRepository.save(entryOut);

    // 2. Call API
    mockMvc.perform(get("/api/reports/stock-summary")
        .header("Authorization", jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].currentStock").value(7.0)); // 10 - 3 = 7
  }

  @Test
  void getPartyOutstanding_ShouldReturnCorrectBalance() throws Exception {
    // 1. Setup GL Data (Sales Invoice -> Debit AR)
    GLTransaction transaction = GLTransaction.builder()
        .date(LocalDate.now())
        .refType(GLTransaction.ReferenceType.SALES_INVOICE)
        .refId(UUID.randomUUID())
        .party(testParty)
        .description("Sales Invoice")
        .build();
    // AR Line (Debit 1000)
    GLLine arLine = GLLine.builder()
        .transaction(transaction)
        .accountHead("ACCOUNTS_RECEIVABLE")
        .debit(new BigDecimal("1000.00"))
        .credit(BigDecimal.ZERO)
        .build();
    
    transaction.getLines().add(arLine);
    glTransactionRepository.save(transaction);

    // 2. Call API
    mockMvc.perform(get("/api/reports/party-outstanding")
        .header("Authorization", jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].partyName").value("Test Customer"))
        .andExpect(jsonPath("$[0].netBalance").value(1000.0));
  }

  @Test
  void getGstr3b_ShouldReturnCorrectData() throws Exception {
    // 1. Setup Sales Invoice
    com.finventory.model.SalesInvoice salesInvoice = com.finventory.model.SalesInvoice.builder()
        .invoiceNumber("INV-SALES-001")
        .invoiceDate(LocalDate.now())
        .party(testParty)
        .warehouse(testWarehouse)
        .totalTaxableAmount(new BigDecimal("100.00"))
        .totalTaxAmount(new BigDecimal("18.00"))
        .totalCgstAmount(new BigDecimal("9.00"))
        .totalSgstAmount(new BigDecimal("9.00"))
        .totalIgstAmount(BigDecimal.ZERO)
        .grandTotal(new BigDecimal("118.00"))
        .build();
    salesInvoiceRepository.save(salesInvoice);

    // 2. Setup Purchase Invoice
    com.finventory.model.PurchaseInvoice purchaseInvoice = com.finventory.model.PurchaseInvoice.builder()
        .invoiceNumber("INV-PUR-001")
        .invoiceDate(LocalDate.now())
        .party(testParty)
        .warehouse(testWarehouse)
        .totalTaxableAmount(new BigDecimal("50.00"))
        .totalTaxAmount(new BigDecimal("9.00"))
        .totalCgstAmount(new BigDecimal("4.50"))
        .totalSgstAmount(new BigDecimal("4.50"))
        .totalIgstAmount(BigDecimal.ZERO)
        .grandTotal(new BigDecimal("59.00"))
        .build();
    purchaseInvoiceRepository.save(purchaseInvoice);

    // 3. Call API
    mockMvc.perform(get("/api/reports/gstr-3b")
        .header("Authorization", jwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outwardTaxableValue").value(100.0))
        .andExpect(jsonPath("$.outwardCgst").value(9.0))
        .andExpect(jsonPath("$.outwardSgst").value(9.0))
        .andExpect(jsonPath("$.itcCgst").value(4.5))
        .andExpect(jsonPath("$.itcSgst").value(4.5))
        .andExpect(jsonPath("$.netTaxPayable").value(9.0));
  }
}
