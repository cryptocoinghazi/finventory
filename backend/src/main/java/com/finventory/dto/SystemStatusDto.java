package com.finventory.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDto {
    private String app;
    private OffsetDateTime serverTime;
    private boolean dbUp;
    private String dbError;

    private long items;
    private long parties;
    private long warehouses;

    private long salesInvoices;
    private long purchaseInvoices;
    private long salesReturns;
    private long purchaseReturns;
    private long stockAdjustments;

    private long salesInvoicesToday;
    private long purchaseInvoicesToday;
    private long salesReturnsToday;
    private long purchaseReturnsToday;
    private long stockAdjustmentsToday;
}
