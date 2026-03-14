package com.finventory.dto;

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
public class PartyLedgerEntryDto {
    private LocalDate date;
    private String refType;
    private UUID refId;
    private String description;
    private BigDecimal amount;
}
