package com.finventory.service;

import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.dto.PurchaseInvoiceLineDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.PurchaseInvoice;
import com.finventory.model.PurchaseInvoiceLine;
import com.finventory.model.SequenceType;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.PurchaseInvoiceRepository;
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
public class PurchaseInvoiceService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final PartyRepository partyRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final StockPostingService stockPostingService;
    private final GLPostingService glPostingService;

    @Transactional
    public PurchaseInvoiceDto createPurchaseInvoice(PurchaseInvoiceDto dto) {
        Party party =
                partyRepository
                        .findById(dto.getPartyId())
                        .orElseThrow(() -> new EntityNotFoundException("Party not found"));

        if (party.getType() != Party.PartyType.VENDOR) {
            throw new IllegalArgumentException("Party must be a VENDOR for purchase invoice");
        }

        Warehouse warehouse =
                warehouseRepository
                        .findById(dto.getWarehouseId())
                        .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        PurchaseInvoice purchaseInvoice =
                PurchaseInvoice.builder()
                        .invoiceNumber(
                                dto.getInvoiceNumber() != null
                                        ? dto.getInvoiceNumber()
                                        : sequenceGeneratorService.generateSequence(
                                                SequenceType.PURCHASE_INVOICE,
                                                warehouse,
                                                dto.getInvoiceDate()))
                        .vendorInvoiceNumber(dto.getVendorInvoiceNumber())
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
        // Fallback to extracting from GSTIN if stateCode is null
        if (partyState == null && party.getGstin() != null && party.getGstin().length() >= 2) {
            partyState = party.getGstin().substring(0, 2);
        }
        String warehouseState = warehouse.getStateCode();

        boolean isInterState = false;
        if (partyState != null && warehouseState != null) {
            isInterState = !partyState.equalsIgnoreCase(warehouseState);
        }

        for (PurchaseInvoiceLineDto lineDto : dto.getLines()) {
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

            PurchaseInvoiceLine line =
                    PurchaseInvoiceLine.builder()
                            .purchaseInvoice(purchaseInvoice)
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

            purchaseInvoice.getLines().add(line);

            totalTaxable = totalTaxable.add(lineAmount);
            totalTax = totalTax.add(lineTaxAmount);
            totalCgst = totalCgst.add(cgst);
            totalSgst = totalSgst.add(sgst);
            totalIgst = totalIgst.add(igst);
        }

        purchaseInvoice.setTotalTaxableAmount(totalTaxable);
        purchaseInvoice.setTotalTaxAmount(totalTax);
        purchaseInvoice.setTotalCgstAmount(totalCgst);
        purchaseInvoice.setTotalSgstAmount(totalSgst);
        purchaseInvoice.setTotalIgstAmount(totalIgst);
        purchaseInvoice.setGrandTotal(totalTaxable.add(totalTax));

        PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(purchaseInvoice);

        // Post to Stock (IN)
        for (PurchaseInvoiceLine line : savedInvoice.getLines()) {
            stockPostingService.postStockIn(
                    savedInvoice.getInvoiceDate(),
                    line.getItem(),
                    savedInvoice.getWarehouse(),
                    line.getQuantity(),
                    StockLedgerEntry.ReferenceType.PURCHASE_INVOICE,
                    savedInvoice.getId());
        }

        // Post to GL
        glPostingService.postPurchaseInvoice(
                savedInvoice.getInvoiceDate(),
                savedInvoice.getId(),
                savedInvoice.getParty(),
                savedInvoice.getTotalTaxableAmount(),
                savedInvoice.getTotalCgstAmount(),
                savedInvoice.getTotalSgstAmount(),
                savedInvoice.getTotalIgstAmount(),
                savedInvoice.getGrandTotal());

        return mapToDto(savedInvoice);
    }

    public PurchaseInvoiceDto getPurchaseInvoice(UUID id) {
        PurchaseInvoice invoice =
                purchaseInvoiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        return mapToDto(invoice);
    }

    public List<PurchaseInvoiceDto> getAllPurchaseInvoices(
            InvoicePaymentStatus paymentStatus, LocalDate fromDate, LocalDate toDate) {
        return purchaseInvoiceRepository.findAllWithFilters(paymentStatus, fromDate, toDate).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseInvoiceDto updatePaymentStatus(UUID id, InvoicePaymentStatus status) {
        PurchaseInvoice invoice =
                purchaseInvoiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        invoice.setPaymentStatus(status);
        return mapToDto(purchaseInvoiceRepository.save(invoice));
    }

    @Transactional
    public PurchaseInvoiceDto applyPayment(
            UUID id, InvoicePaymentStatus status, BigDecimal paymentAmount) {
        PurchaseInvoice invoice =
                purchaseInvoiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal paidSoFar = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
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

        return mapToDto(purchaseInvoiceRepository.save(invoice));
    }

    private PurchaseInvoiceDto mapToDto(PurchaseInvoice invoice) {
        return PurchaseInvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .vendorInvoiceNumber(invoice.getVendorInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .partyId(invoice.getParty().getId())
                .partyName(invoice.getParty().getName())
                .warehouseId(invoice.getWarehouse().getId())
                .warehouseName(invoice.getWarehouse().getName())
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
                .lines(
                        invoice.getLines().stream()
                                .map(
                                        line ->
                                                PurchaseInvoiceLineDto.builder()
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
                                .toList())
                .build();
    }
}
