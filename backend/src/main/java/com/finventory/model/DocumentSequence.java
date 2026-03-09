package com.finventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "document_sequences",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"sequence_type", "warehouse_id", "financial_year"})
        })
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sequence_type", nullable = false)
    private SequenceType sequenceType;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "financial_year", nullable = false)
    private String financialYear;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;

    @Column(name = "prefix", nullable = false)
    private String prefix;

    @Column(name = "suffix")
    private String suffix;
}
