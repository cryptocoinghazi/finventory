package com.finventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstRegisterEntryDto {
  private String invoiceNumber;
  private LocalDate invoiceDate;
  private String partyName;
  private String partyGstin;
  private String placeOfSupply; // State Code
  private String invoiceType; // B2B, B2C, etc.
  private BigDecimal taxableValue;
  private BigDecimal cgstAmount;
  private BigDecimal sgstAmount;
  private BigDecimal igstAmount;
  private BigDecimal totalAmount;
}
