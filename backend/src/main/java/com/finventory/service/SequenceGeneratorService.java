package com.finventory.service;

import com.finventory.model.DocumentSequence;
import com.finventory.model.SequenceType;
import com.finventory.model.Warehouse;
import com.finventory.repository.DocumentSequenceRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final DocumentSequenceRepository documentSequenceRepository;

    @Transactional
    public String generateSequence(SequenceType sequenceType, Warehouse warehouse, LocalDate date) {
        String financialYear = getFinancialYear(date);

        DocumentSequence sequence =
                documentSequenceRepository
                        .findBySequenceTypeAndWarehouseIdAndFinancialYear(
                                sequenceType, warehouse.getId(), financialYear)
                        .orElse(null);

        if (sequence == null) {
            sequence = createNewSequence(sequenceType, warehouse, financialYear);
            // Save immediately to get an ID and lock it?
            // Or just proceed. If we proceed, save() at the end will insert.
        }

        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        documentSequenceRepository.save(sequence);

        return formatSequence(sequence, warehouse);
    }

    private DocumentSequence createNewSequence(
            SequenceType sequenceType, Warehouse warehouse, String financialYear) {
        String prefix = getDefaultPrefix(sequenceType);

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

        int startYear;
        int endYear;

        if (month >= 4) { // April onwards
            startYear = year;
            endYear = year + 1;
        } else {
            startYear = year - 1;
            endYear = year;
        }

        return (startYear % 100) + "-" + (endYear % 100);
    }

    private String getDefaultPrefix(SequenceType sequenceType) {
        return switch (sequenceType) {
            case SALES_INVOICE -> "INV";
            case PURCHASE_INVOICE -> "PINV";
            case SALES_RETURN -> "CN"; // Credit Note
            case PURCHASE_RETURN -> "DN"; // Debit Note
        };
    }

    private String formatSequence(DocumentSequence sequence, Warehouse warehouse) {
        // Format: PREFIX/FY/BRANCH/00001
        // Example: INV/24-25/MUM/00001

        String branchCode = getBranchCode(warehouse);
        String sequenceNumber = String.format("%05d", sequence.getCurrentValue());

        return String.format(
                "%s/%s/%s/%s",
                sequence.getPrefix(), sequence.getFinancialYear(), branchCode, sequenceNumber);
    }

    private String getBranchCode(Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().isEmpty()) {
            return "HO"; // Head Office default
        }
        String name = warehouse.getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return name.length() > 3 ? name.substring(0, 3) : name;
    }
}
