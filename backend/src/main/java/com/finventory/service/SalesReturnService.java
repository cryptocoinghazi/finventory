package com.finventory.service;

import com.finventory.dto.SalesReturnDto;
import com.finventory.dto.SalesReturnLineDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.SalesInvoice;
import com.finventory.model.SalesInvoiceLine;
import com.finventory.model.SalesReturn;
import com.finventory.model.SalesReturnLine;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.SalesReturnRepository;
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
public class SalesReturnService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private final SalesReturnRepository salesReturnRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PartyRepository partyRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final StockPostingService stockPostingService;
    private final GLPostingService glPostingService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional
    public SalesReturnDto createSalesReturn(SalesReturnDto dto) {
        Party party =
                partyRepository
                        .findById(dto.getPartyId())
                        .orElseThrow(() -> new EntityNotFoundException("Party not found"));

        Warehouse warehouse =
                warehouseRepository
                        .findById(dto.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        SalesInvoice salesInvoice = null;
        if (dto.getSalesInvoiceId() != null) {
            salesInvoice =
                    salesInvoiceRepository
                            .findById(dto.getSalesInvoiceId())
                            .orElseThrow(
                                    () -> new EntityNotFoundException("Sales Invoice not found"));

            // Validate return quantities against original invoice quantities
            Map<java.util.UUID, BigDecimal> invoiceItemQuantities =
                    salesInvoice.getLines().stream()
                            .collect(
                                    Collectors.toMap(
                                            line -> line.getItem().getId(),
                                            SalesInvoiceLine::getQuantity));

            List<SalesReturn> existingReturns =
                    salesReturnRepository.findBySalesInvoiceId(salesInvoice.getId());
            Map<java.util.UUID, BigDecimal> returnedItemQuantities = new HashMap<>();

            for (SalesReturn existingReturn : existingReturns) {
                for (SalesReturnLine line : existingReturn.getLines()) {
                    returnedItemQuantities.merge(
                            line.getItem().getId(), line.getQuantity(), BigDecimal::add);
                }
            }

            for (SalesReturnLineDto lineDto : dto.getLines()) {
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
                            com.finventory.model.SequenceType.SALES_RETURN,
                            warehouse,
                            dto.getReturnDate());
        }

        SalesReturn salesReturn =
                SalesReturn.builder()
                        .returnNumber(returnNumber)
                        .salesInvoice(salesInvoice)
                        .returnDate(dto.getReturnDate())
                        .party(party)
                        .warehouse(warehouse)
                        .lines(new ArrayList<>())
                        .build();

        // Determine Tax Type (Inter-state vs Intra-state)
        String partyState = party.getStateCode();
        if (partyState == null && party.getGstin() != null && party.getGstin().length() >= 2) {
            partyState = party.getGstin().substring(0, 2);
        }
        String warehouseState = warehouse.getStateCode();

        boolean isInterState = false;
        if (partyState != null && warehouseState != null) {
            isInterState = !partyState.equalsIgnoreCase(warehouseState);
        }

        calculateReturnTotals(salesReturn, dto, isInterState);

        SalesReturn savedReturn = salesReturnRepository.save(salesReturn);

        // Stock Posting (Stock IN)
        for (SalesReturnLine line : savedReturn.getLines()) {
            stockPostingService.postStockIn(
                    savedReturn.getReturnDate(),
                    line.getItem(),
                    savedReturn.getWarehouse(),
                    line.getQuantity(),
                    StockLedgerEntry.ReferenceType.SALES_RETURN,
                    savedReturn.getId());
        }

        // GL Posting
        glPostingService.postSalesReturn(
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
            SalesReturn salesReturn, SalesReturnDto dto, boolean isInterState) {
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (SalesReturnLineDto lineDto : dto.getLines()) {
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

            SalesReturnLine line =
                    SalesReturnLine.builder()
                            .salesReturn(salesReturn)
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

            salesReturn.getLines().add(line);

            totalTaxable = totalTaxable.add(lineAmount);
            totalTax = totalTax.add(lineTaxAmount);
            totalCgst = totalCgst.add(cgst);
            totalSgst = totalSgst.add(sgst);
            totalIgst = totalIgst.add(igst);
        }

        salesReturn.setTotalTaxableAmount(totalTaxable);
        salesReturn.setTotalTaxAmount(totalTax);
        salesReturn.setTotalCgstAmount(totalCgst);
        salesReturn.setTotalSgstAmount(totalSgst);
        salesReturn.setTotalIgstAmount(totalIgst);
        salesReturn.setGrandTotal(totalTaxable.add(totalTax));
    }

    @Transactional(readOnly = true)
    public java.util.List<SalesReturnDto> getAllSalesReturns() {
        return salesReturnRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public SalesReturnDto getSalesReturn(java.util.UUID id) {
        return salesReturnRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Sales Return not found"));
    }

    private SalesReturnDto mapToDto(SalesReturn salesReturn) {
        return SalesReturnDto.builder()
                .id(salesReturn.getId())
                .returnNumber(salesReturn.getReturnNumber())
                .salesInvoiceId(
                        salesReturn.getSalesInvoice() != null
                                ? salesReturn.getSalesInvoice().getId()
                                : null)
                .returnDate(salesReturn.getReturnDate())
                .partyId(salesReturn.getParty().getId())
                .partyName(salesReturn.getParty().getName())
                .warehouseId(salesReturn.getWarehouse().getId())
                .warehouseName(salesReturn.getWarehouse().getName())
                .totalTaxableAmount(salesReturn.getTotalTaxableAmount())
                .totalTaxAmount(salesReturn.getTotalTaxAmount())
                .totalCgstAmount(salesReturn.getTotalCgstAmount())
                .totalSgstAmount(salesReturn.getTotalSgstAmount())
                .totalIgstAmount(salesReturn.getTotalIgstAmount())
                .grandTotal(salesReturn.getGrandTotal())
                .lines(salesReturn.getLines().stream().map(this::mapLineToDto).toList())
                .build();
    }

    private SalesReturnLineDto mapLineToDto(SalesReturnLine line) {
        return SalesReturnLineDto.builder()
                .id(line.getId())
                .itemId(line.getItem().getId())
                .itemName(line.getItem().getName())
                .itemCode(line.getItem().getCode())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .taxRate(line.getTaxRate())
                .taxAmount(line.getTaxAmount())
                .lineTotal(line.getLineTotal())
                .build();
    }
}
