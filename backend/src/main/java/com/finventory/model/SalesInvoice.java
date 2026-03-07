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
@Table(name = "sales_invoices")
public class SalesInvoice {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "invoice_number", nullable = false, unique = true)
  private String invoiceNumber;

  @Column(name = "invoice_date", nullable = false)
  private LocalDate invoiceDate;

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

  @Column(name = "grand_total", nullable = false)
  private BigDecimal grandTotal;

  @Builder.Default
  @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SalesInvoiceLine> lines = new ArrayList<>();
}
