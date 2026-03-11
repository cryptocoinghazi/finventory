package com.finventory.service;

import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.SalesInvoice;
import com.finventory.model.SalesInvoiceLine;
import com.finventory.model.SequenceType;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesInvoiceService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PartyRepository partyRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final StockPostingService stockPostingService;
    private final GLPostingService glPostingService;

    @Transactional
    public SalesInvoiceDto createSalesInvoice(SalesInvoiceDto dto) {
        Party party =
                partyRepository
                        .findById(dto.getPartyId())
                        .orElseThrow(() -> new EntityNotFoundException("Party not found"));

        Warehouse warehouse =
                warehouseRepository
                        .findById(dto.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        SalesInvoice salesInvoice =
                SalesInvoice.builder()
                        .invoiceNumber(
                                dto.getInvoiceNumber() != null
                                        ? dto.getInvoiceNumber()
                                        : sequenceGeneratorService.generateSequence(
                                                SequenceType.SALES_INVOICE,
                                                warehouse,
                                                dto.getInvoiceDate()))
                        .invoiceDate(dto.getInvoiceDate())
                        .party(party)
                        .warehouse(warehouse)
                        .paymentStatus(
                                dto.getPaymentStatus() != null
                                        ? dto.getPaymentStatus()
                                        : InvoicePaymentStatus.PENDING)
                        .lines(new ArrayList<>())
                        .build();

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

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

        for (SalesInvoiceLineDto lineDto : dto.getLines()) {
            Item item =
                    itemRepository
                            .findById(lineDto.getItemId())
                            .orElseThrow(
                                    () ->
                                            new EntityNotFoundException(
                                                    "Item not found: " + lineDto.getItemId()));

            BigDecimal quantity = lineDto.getQuantity();
            BigDecimal unitPrice = lineDto.getUnitPrice();
            // Use item tax rate if not provided in DTO (or if you want to enforce current rate)
            // Usually, tax rate is snapshot at time of invoice.
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
                cgst = lineTaxAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                sgst = lineTaxAmount.subtract(cgst);
            }

            BigDecimal lineTotal = lineAmount.add(lineTaxAmount);

            SalesInvoiceLine line =
                    SalesInvoiceLine.builder()
                            .salesInvoice(salesInvoice)
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

            salesInvoice.getLines().add(line);

            totalTaxable = totalTaxable.add(lineAmount);
            totalTax = totalTax.add(lineTaxAmount);
            totalCgst = totalCgst.add(cgst);
            totalSgst = totalSgst.add(sgst);
            totalIgst = totalIgst.add(igst);
        }

        salesInvoice.setTotalTaxableAmount(totalTaxable);
        salesInvoice.setTotalTaxAmount(totalTax);
        salesInvoice.setTotalCgstAmount(totalCgst);
        salesInvoice.setTotalSgstAmount(totalSgst);
        salesInvoice.setTotalIgstAmount(totalIgst);
        salesInvoice.setGrandTotal(totalTaxable.add(totalTax));

        SalesInvoice savedInvoice = salesInvoiceRepository.save(salesInvoice);

        // --- POSTING LOGIC ---

        // 1. Post to GL
        glPostingService.postSalesInvoice(
                savedInvoice.getInvoiceDate(),
                savedInvoice.getId(),
                savedInvoice.getParty(),
                savedInvoice.getTotalTaxableAmount(),
                savedInvoice.getTotalCgstAmount(),
                savedInvoice.getTotalSgstAmount(),
                savedInvoice.getTotalIgstAmount(),
                savedInvoice.getGrandTotal());

        // 2. Post to Stock Ledger (for each line)
        for (SalesInvoiceLine line : savedInvoice.getLines()) {
            stockPostingService.postStockOut(
                    savedInvoice.getInvoiceDate(),
                    line.getItem(),
                    savedInvoice.getWarehouse(),
                    line.getQuantity(),
                    StockLedgerEntry.ReferenceType.SALES_INVOICE,
                    savedInvoice.getId());
        }

        return mapToDto(savedInvoice);
    }

    public SalesInvoiceDto getSalesInvoice(UUID id) {
        SalesInvoice invoice =
                salesInvoiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        return mapToDto(invoice);
    }

    public List<SalesInvoiceDto> getAllSalesInvoices(
            InvoicePaymentStatus paymentStatus, LocalDate fromDate, LocalDate toDate) {
        return salesInvoiceRepository.findAllWithFilters(paymentStatus, fromDate, toDate).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalesInvoiceDto updatePaymentStatus(UUID id, InvoicePaymentStatus status) {
        SalesInvoice invoice =
                salesInvoiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        invoice.setPaymentStatus(status);
        return mapToDto(salesInvoiceRepository.save(invoice));
    }

    @Transactional
    public SalesInvoiceDto applyPayment(
            UUID id, InvoicePaymentStatus status, BigDecimal paymentAmount) {
        SalesInvoice invoice =
                salesInvoiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        BigDecimal grandTotal =
                invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal paidSoFar =
                invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = grandTotal.subtract(paidSoFar);

        if (status == null) {
            throw new IllegalArgumentException("Payment status is required");
        }

        if (paymentAmount == null) {
            paymentAmount = BigDecimal.ZERO;
        }

        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount must be >= 0");
        }

        if (paymentAmount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds outstanding balance");
        }

        if (status == InvoicePaymentStatus.PENDING) {
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setPaymentStatus(InvoicePaymentStatus.PENDING);
        } else {
            BigDecimal nextPaid =
                    status == InvoicePaymentStatus.PAID ? grandTotal : paidSoFar.add(paymentAmount);
            invoice.setPaidAmount(nextPaid);

            if (nextPaid.compareTo(BigDecimal.ZERO) == 0) {
                invoice.setPaymentStatus(InvoicePaymentStatus.PENDING);
            } else if (nextPaid.compareTo(grandTotal) >= 0) {
                invoice.setPaidAmount(grandTotal);
                invoice.setPaymentStatus(InvoicePaymentStatus.PAID);
            } else {
                invoice.setPaymentStatus(InvoicePaymentStatus.PARTIAL);
            }
        }

        return mapToDto(salesInvoiceRepository.save(invoice));
    }

    private SalesInvoiceDto mapToDto(SalesInvoice invoice) {
        List<SalesInvoiceLineDto> lineDtos =
                invoice.getLines().stream()
                        .map(
                                line ->
                                        SalesInvoiceLineDto.builder()
                                                .id(line.getId())
                                                .itemId(line.getItem().getId())
                                                .itemName(line.getItem().getName())
                                                .itemCode(line.getItem().getCode())
                                                .quantity(line.getQuantity())
                                                .unitPrice(line.getUnitPrice())
                                                .taxRate(line.getTaxRate())
                                                .taxAmount(line.getTaxAmount())
                                                .cgstAmount(line.getCgstAmount())
                                                .sgstAmount(line.getSgstAmount())
                                                .igstAmount(line.getIgstAmount())
                                                .lineTotal(line.getLineTotal())
                                                .build())
                        .collect(Collectors.toList());

        return SalesInvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .partyId(invoice.getParty().getId())
                .partyName(invoice.getParty().getName())
                .warehouseId(invoice.getWarehouse().getId())
                .warehouseName(invoice.getWarehouse().getName())
                .lines(lineDtos)
                .totalTaxableAmount(invoice.getTotalTaxableAmount())
                .totalTaxAmount(invoice.getTotalTaxAmount())
                .totalCgstAmount(invoice.getTotalCgstAmount())
                .totalSgstAmount(invoice.getTotalSgstAmount())
                .totalIgstAmount(invoice.getTotalIgstAmount())
                .grandTotal(invoice.getGrandTotal())
                .paidAmount(invoice.getPaidAmount())
                .balanceAmount(
                        invoice.getGrandTotal() != null
                                ? invoice.getGrandTotal()
                                        .subtract(
                                                invoice.getPaidAmount() != null
                                                        ? invoice.getPaidAmount()
                                                        : BigDecimal.ZERO)
                                : BigDecimal.ZERO)
                .paymentStatus(invoice.getPaymentStatus())
                .build();
    }
}
