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
public class ActivityFeedEntryDto {
    private String kind;
    private UUID id;
    private LocalDate date;
    private String title;
    private String subtitle;
    private BigDecimal amount;
    private String href;
}
