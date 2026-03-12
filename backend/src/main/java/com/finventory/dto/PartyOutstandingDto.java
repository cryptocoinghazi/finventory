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
    private String phone;
    private String gstin;
    private BigDecimal totalReceivable; // Debit sum for Customers
    private BigDecimal totalPayable; // Credit sum for Vendors
    private BigDecimal netBalance; // Debit - Credit
    private BigDecimal age0to30;
    private BigDecimal age31to60;
    private BigDecimal age61to90;
    private BigDecimal age90Plus;

    public PartyOutstandingDto(
            UUID partyId,
            String partyName,
            String partyType,
            BigDecimal totalReceivable,
            BigDecimal totalPayable,
            BigDecimal netBalance) {
        this.partyId = partyId;
        this.partyName = partyName;
        this.partyType = partyType;
        this.totalReceivable = totalReceivable;
        this.totalPayable = totalPayable;
        this.netBalance = netBalance;
    }
}
