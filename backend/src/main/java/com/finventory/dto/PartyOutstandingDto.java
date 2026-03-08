package com.finventory.dto;

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
public class PartyOutstandingDto {
  private UUID partyId;
  private String partyName;
  private String partyType; // CUSTOMER, VENDOR
  private BigDecimal totalReceivable; // Debit sum for Customers
  private BigDecimal totalPayable;    // Credit sum for Vendors
  private BigDecimal netBalance;      // Debit - Credit
}
