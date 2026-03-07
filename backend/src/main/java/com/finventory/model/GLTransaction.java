package com.finventory.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "gl_transactions")
public class GLTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  @Column(name = "ref_type", nullable = false)
  private ReferenceType refType;

  @Column(name = "ref_id", nullable = false)
  private UUID refId;

  private String description;

  @Builder.Default
  @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GLLine> lines = new ArrayList<>();

  public enum ReferenceType {
    SALES_INVOICE,
    PURCHASE_INVOICE,
    SALES_RETURN,
    PURCHASE_RETURN,
    PAYMENT_RECEIPT,
    PAYMENT_MADE
  }
}
