package com.finventory.service;

import com.finventory.model.DocumentSequence;
import com.finventory.model.SequenceType;
import com.finventory.model.Warehouse;
import com.finventory.repository.DocumentSequenceRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private static final int APRIL = 4;
    private static final int YEAR_MODULO = 100;

    private final DocumentSequenceRepository documentSequenceRepository;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public String generateSequence(SequenceType sequenceType, Warehouse warehouse, LocalDate date) {
        String financialYear = getFinancialYear(date);

        DocumentSequence sequence =
                documentSequenceRepository
                        .findBySequenceTypeAndWarehouseIdAndFinancialYear(
                                sequenceType, warehouse.getId(), financialYear)
                        .orElse(null);

        if (sequence == null) {
            // Handle concurrent creation
            try {
                TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tt.execute(
                        status -> {
                            // Double-check inside new transaction
                            Optional<DocumentSequence> existing =
                                    documentSequenceRepository
                                            .findBySequenceTypeAndWarehouseIdAndFinancialYear(
                                                    sequenceType, warehouse.getId(), financialYear);
                            if (existing.isEmpty()) {
                                DocumentSequence newSeq =
                                        createNewSequence(sequenceType, warehouse, financialYear);
                                documentSequenceRepository.save(newSeq);
                            }
                            return null;
                        });
            } catch (DataIntegrityViolationException e) {
                // Ignore: Sequence was created by another transaction
            }

            // Fetch again with lock (now it must exist)
            sequence =
                    documentSequenceRepository
                            .findBySequenceTypeAndWarehouseIdAndFinancialYear(
                                    sequenceType, warehouse.getId(), financialYear)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Sequence should exist after creation attempt"));
        }

        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        documentSequenceRepository.save(sequence);

        return formatSequence(sequence, warehouse);
    }

    private DocumentSequence createNewSequence(
            SequenceType sequenceType, Warehouse warehouse, String financialYear) {
        String prefix = sequenceType.getCode();

        return DocumentSequence.builder()
                .sequenceType(sequenceType)
                .warehouseId(warehouse.getId())
                .financialYear(financialYear)
                .currentValue(0L)
                .prefix(prefix)
                .build();
    }

    private String getFinancialYear(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        int endYear;

        if (month >= APRIL) { // April onwards
            endYear = year + 1;
        } else {
            endYear = year;
        }

        return "FY" + (endYear % YEAR_MODULO);
    }

    private String formatSequence(DocumentSequence sequence, Warehouse warehouse) {
        // Format: FY25-B1-S-000001
        String branchCode = warehouse.getCode() != null ? warehouse.getCode() : "HO";
        String sequenceNumber = String.format("%06d", sequence.getCurrentValue());

        return String.format(
                "%s-%s-%s-%s",
                sequence.getFinancialYear(),
                branchCode,
                sequence.getPrefix(),
                sequenceNumber);
    }
}
