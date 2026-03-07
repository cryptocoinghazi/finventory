package com.finventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "stock_ledger_entries")
public class StockLedgerEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private LocalDate date;

  @ManyToOne
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @ManyToOne
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(name = "qty_in", nullable = false)
  private BigDecimal qtyIn;

  @Column(name = "qty_out", nullable = false)
  private BigDecimal qtyOut;

  @Enumerated(EnumType.STRING)
  @Column(name = "ref_type", nullable = false)
  private ReferenceType refType;

  @Column(name = "ref_id", nullable = false)
  private UUID refId;

  public enum ReferenceType {
    SALES_INVOICE,
    PURCHASE_INVOICE,
    SALES_RETURN,
    PURCHASE_RETURN,
    STOCK_ADJUSTMENT
  }
}
