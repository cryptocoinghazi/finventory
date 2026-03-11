package com.finventory.service;

import com.finventory.dto.PurchaseReturnDto;
import com.finventory.dto.PurchaseReturnLineDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.PurchaseInvoice;
import com.finventory.model.PurchaseInvoiceLine;
import com.finventory.model.PurchaseReturn;
import com.finventory.model.PurchaseReturnLine;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.PurchaseInvoiceRepository;
import com.finventory.repository.PurchaseReturnRepository;
import com.finventory.repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final PartyRepository partyRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockPostingService stockPostingService;
    private final GLPostingService glPostingService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional
    public PurchaseReturnDto createPurchaseReturn(PurchaseReturnDto dto) {
        Party party =
                partyRepository
                        .findById(dto.getPartyId())
                        .orElseThrow(() -> new EntityNotFoundException("Party not found"));

        if (party.getType() != Party.PartyType.VENDOR) {
            throw new IllegalArgumentException("Party must be a VENDOR for purchase return");
        }

        Warehouse warehouse =
                warehouseRepository
                        .findById(dto.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        PurchaseInvoice purchaseInvoice = null;
        if (dto.getPurchaseInvoiceId() != null) {
            purchaseInvoice =
                    purchaseInvoiceRepository
                            .findById(dto.getPurchaseInvoiceId())
                            .orElseThrow(
                                    () ->
                                            new EntityNotFoundException(
                                                    "Purchase Invoice not found"));

            // Validate return quantities against original invoice quantities
            Map<java.util.UUID, BigDecimal> invoiceItemQuantities =
                    purchaseInvoice.getLines().stream()
                            .collect(
                                    Collectors.toMap(
                                            line -> line.getItem().getId(),
                                            PurchaseInvoiceLine::getQuantity));

            List<PurchaseReturn> existingReturns =
                    purchaseReturnRepository.findByPurchaseInvoiceId(purchaseInvoice.getId());
            Map<java.util.UUID, BigDecimal> returnedItemQuantities = new HashMap<>();

            for (PurchaseReturn existingReturn : existingReturns) {
                for (PurchaseReturnLine line : existingReturn.getLines()) {
                    returnedItemQuantities.merge(
                            line.getItem().getId(), line.getQuantity(), BigDecimal::add);
                }
            }

            for (PurchaseReturnLineDto lineDto : dto.getLines()) {
                BigDecimal originalQuantity =
                        invoiceItemQuantities.getOrDefault(lineDto.getItemId(), BigDecimal.ZERO);
                BigDecimal alreadyReturned =
                        returnedItemQuantities.getOrDefault(lineDto.getItemId(), BigDecimal.ZERO);
                BigDecimal currentReturn = lineDto.getQuantity();

                if (alreadyReturned.add(currentReturn).compareTo(originalQuantity) > 0) {
                    throw new IllegalArgumentException(
                            "Return quantity exceeds available quantity for item: "
                                    + lineDto.getItemId());
                }
            }
        }

        String returnNumber = dto.getReturnNumber();
        if (returnNumber == null || returnNumber.trim().isEmpty()) {
            returnNumber =
                    sequenceGeneratorService.generateSequence(
                            com.finventory.model.SequenceType.PURCHASE_RETURN,
                            warehouse,
                            dto.getReturnDate());
        }

        PurchaseReturn purchaseReturn =
                PurchaseReturn.builder()
                        .returnNumber(returnNumber)
                        .purchaseInvoice(purchaseInvoice)
                        .returnDate(dto.getReturnDate())
                        .party(party)
                        .warehouse(warehouse)
                        .lines(new ArrayList<>())
                        .build();

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        // Determine Tax Type (Inter-state vs Intra-state)
        String partyState = party.getStateCode();
        // Fallback to extracting from GSTIN if stateCode is null
        if (partyState == null && party.getGstin() != null && party.getGstin().length() >= 2) {
            partyState = party.getGstin().substring(0, 2);
        }
        String warehouseState = warehouse.getStateCode();

        boolean isInterState = false;
        if (partyState != null && warehouseState != null) {
            isInterState = !partyState.equalsIgnoreCase(warehouseState);
        }

        calculateReturnTotals(purchaseReturn, dto, isInterState);

        PurchaseReturn savedReturn = purchaseReturnRepository.save(purchaseReturn);

        // Post to Stock (OUT)
        for (PurchaseReturnLine line : savedReturn.getLines()) {
            stockPostingService.postStockOut(
                    savedReturn.getReturnDate(),
                    line.getItem(),
                    savedReturn.getWarehouse(),
                    line.getQuantity(),
                    StockLedgerEntry.ReferenceType.PURCHASE_RETURN,
                    savedReturn.getId());
        }

        // Post to GL
        glPostingService.postPurchaseReturn(
                savedReturn.getReturnDate(),
                savedReturn.getId(),
                savedReturn.getParty(),
                savedReturn.getTotalTaxableAmount(),
                savedReturn.getTotalCgstAmount(),
                savedReturn.getTotalSgstAmount(),
                savedReturn.getTotalIgstAmount(),
                savedReturn.getGrandTotal());

        return mapToDto(savedReturn);
    }

    private void calculateReturnTotals(
            PurchaseReturn purchaseReturn, PurchaseReturnDto dto, boolean isInterState) {
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (PurchaseReturnLineDto lineDto : dto.getLines()) {
            Item item =
                    itemRepository
                            .findById(lineDto.getItemId())
                            .orElseThrow(
                                    () ->
                                            new EntityNotFoundException(
                                                    "Item not found: " + lineDto.getItemId()));

            BigDecimal quantity = lineDto.getQuantity();
            BigDecimal unitPrice = lineDto.getUnitPrice();
            BigDecimal taxRate =
                    lineDto.getTaxRate() != null ? lineDto.getTaxRate() : item.getTaxRate();

            BigDecimal lineAmount = quantity.multiply(unitPrice);
            BigDecimal lineTaxAmount =
                    lineAmount.multiply(taxRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal cgst = BigDecimal.ZERO;
            BigDecimal sgst = BigDecimal.ZERO;
            BigDecimal igst = BigDecimal.ZERO;

            if (isInterState) {
                igst = lineTaxAmount;
            } else {
                cgst = lineTaxAmount.divide(TWO, 2, RoundingMode.HALF_UP);
                sgst = lineTaxAmount.subtract(cgst);
            }

            BigDecimal lineTotal = lineAmount.add(lineTaxAmount);

            PurchaseReturnLine line =
                    PurchaseReturnLine.builder()
                            .purchaseReturn(purchaseReturn)
                            .item(item)
                            .quantity(quantity)
                            .unitPrice(unitPrice)
                            .taxRate(taxRate)
                            .taxAmount(lineTaxAmount)
                            .cgstAmount(cgst)
                            .sgstAmount(sgst)
                            .igstAmount(igst)
                            .lineTotal(lineTotal)
                            .build();

            purchaseReturn.getLines().add(line);

            totalTaxable = totalTaxable.add(lineAmount);
            totalTax = totalTax.add(lineTaxAmount);
            totalCgst = totalCgst.add(cgst);
            totalSgst = totalSgst.add(sgst);
            totalIgst = totalIgst.add(igst);
        }

        purchaseReturn.setTotalTaxableAmount(totalTaxable);
        purchaseReturn.setTotalTaxAmount(totalTax);
        purchaseReturn.setTotalCgstAmount(totalCgst);
        purchaseReturn.setTotalSgstAmount(totalSgst);
        purchaseReturn.setTotalIgstAmount(totalIgst);
        purchaseReturn.setGrandTotal(totalTaxable.add(totalTax));
    }

    @Transactional(readOnly = true)
    public java.util.List<PurchaseReturnDto> getAllPurchaseReturns() {
        return purchaseReturnRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseReturnDto getPurchaseReturn(java.util.UUID id) {
        return purchaseReturnRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Purchase Return not found"));
    }

    private PurchaseReturnDto mapToDto(PurchaseReturn returnObj) {
        return PurchaseReturnDto.builder()
                .id(returnObj.getId())
                .returnNumber(returnObj.getReturnNumber())
                .purchaseInvoiceId(
                        returnObj.getPurchaseInvoice() != null
                                ? returnObj.getPurchaseInvoice().getId()
                                : null)
                .returnDate(returnObj.getReturnDate())
                .partyId(returnObj.getParty().getId())
                .partyName(returnObj.getParty().getName())
                .warehouseId(returnObj.getWarehouse().getId())
                .warehouseName(returnObj.getWarehouse().getName())
                .totalTaxableAmount(returnObj.getTotalTaxableAmount())
                .totalTaxAmount(returnObj.getTotalTaxAmount())
                .totalCgstAmount(returnObj.getTotalCgstAmount())
                .totalSgstAmount(returnObj.getTotalSgstAmount())
                .totalIgstAmount(returnObj.getTotalIgstAmount())
                .grandTotal(returnObj.getGrandTotal())
                .lines(
                        returnObj.getLines().stream()
                                .map(
                                        line ->
                                                PurchaseReturnLineDto.builder()
                                                        .id(line.getId())
                                                        .itemId(line.getItem().getId())
                                                        .itemName(line.getItem().getName())
                                                        .itemCode(line.getItem().getCode())
                                                        .quantity(line.getQuantity())
                                                        .unitPrice(line.getUnitPrice())
                                                        .taxRate(line.getTaxRate())
                                                        .taxAmount(line.getTaxAmount())
                                                        .lineTotal(line.getLineTotal())
                                                        .build())
                                .toList())
                .build();
    }
}
