package com.finventory.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_returns")
public class PurchaseReturn {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "return_number", nullable = false, unique = true)
  private String returnNumber;

  @ManyToOne
  @JoinColumn(name = "purchase_invoice_id")
  private PurchaseInvoice purchaseInvoice;

  @Column(name = "return_date", nullable = false)
  private LocalDate returnDate;

  @ManyToOne
  @JoinColumn(name = "party_id", nullable = false)
  private Party party;

  @ManyToOne
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(name = "total_taxable_amount", nullable = false)
  private BigDecimal totalTaxableAmount;

  @Column(name = "total_tax_amount", nullable = false)
  private BigDecimal totalTaxAmount;

  @Column(name = "total_cgst_amount", nullable = false)
  private BigDecimal totalCgstAmount;

  @Column(name = "total_sgst_amount", nullable = false)
  private BigDecimal totalSgstAmount;

  @Column(name = "total_igst_amount", nullable = false)
  private BigDecimal totalIgstAmount;

  @Column(name = "grand_total", nullable = false)
  private BigDecimal grandTotal;

  @Builder.Default
  @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PurchaseReturnLine> lines = new ArrayList<>();
}
