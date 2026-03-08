package com.finventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "purchase_return_lines")
public class PurchaseReturnLine {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "purchase_return_id", nullable = false)
  private PurchaseReturn purchaseReturn;

  @ManyToOne
  @JoinColumn(name = "item_id", nullable = false)
  private Item item;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "tax_rate", nullable = false)
  private BigDecimal taxRate;

  @Column(name = "tax_amount", nullable = false)
  private BigDecimal taxAmount;

  @Column(name = "cgst_amount", nullable = false)
  private BigDecimal cgstAmount;

  @Column(name = "sgst_amount", nullable = false)
  private BigDecimal sgstAmount;

  @Column(name = "igst_amount", nullable = false)
  private BigDecimal igstAmount;

  @Column(name = "line_total", nullable = false)
  private BigDecimal lineTotal;
}
