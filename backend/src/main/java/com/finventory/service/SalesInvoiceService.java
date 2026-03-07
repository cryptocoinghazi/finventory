package com.finventory.service;

import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.SalesInvoice;
import com.finventory.model.SalesInvoiceLine;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  private final SalesInvoiceRepository salesInvoiceRepository;
  private final PartyRepository partyRepository;
  private final ItemRepository itemRepository;
  private final WarehouseRepository warehouseRepository;
  private final StockPostingService stockPostingService;
  private final GLPostingService glPostingService;

  @Transactional
  public SalesInvoiceDto createSalesInvoice(SalesInvoiceDto dto) {
    Party party = partyRepository.findById(dto.getPartyId())
        .orElseThrow(() -> new EntityNotFoundException("Party not found"));

    Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
        .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

    SalesInvoice salesInvoice = SalesInvoice.builder()
        .invoiceNumber(dto.getInvoiceNumber()) // Should generate if null
        .invoiceDate(dto.getInvoiceDate())
        .party(party)
        .warehouse(warehouse)
        .lines(new ArrayList<>())
        .build();

    BigDecimal totalTaxable = BigDecimal.ZERO;
    BigDecimal totalTax = BigDecimal.ZERO;

    for (SalesInvoiceLineDto lineDto : dto.getLines()) {
      Item item = itemRepository.findById(lineDto.getItemId())
          .orElseThrow(() -> new EntityNotFoundException("Item not found: " + lineDto.getItemId()));

      BigDecimal quantity = lineDto.getQuantity();
      BigDecimal unitPrice = lineDto.getUnitPrice();
      // Use item tax rate if not provided in DTO (or if you want to enforce current rate)
      // Usually, tax rate is snapshot at time of invoice.
      BigDecimal taxRate = lineDto.getTaxRate() != null ? lineDto.getTaxRate() : item.getTaxRate();

      BigDecimal lineAmount = quantity.multiply(unitPrice);
      BigDecimal lineTaxAmount = lineAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      BigDecimal lineTotal = lineAmount.add(lineTaxAmount);

      SalesInvoiceLine line = SalesInvoiceLine.builder()
          .salesInvoice(salesInvoice)
          .item(item)
          .quantity(quantity)
          .unitPrice(unitPrice)
          .taxRate(taxRate)
          .taxAmount(lineTaxAmount)
          .lineTotal(lineTotal)
          .build();

      salesInvoice.getLines().add(line);

      totalTaxable = totalTaxable.add(lineAmount);
      totalTax = totalTax.add(lineTaxAmount);
    }

    salesInvoice.setTotalTaxableAmount(totalTaxable);
    salesInvoice.setTotalTaxAmount(totalTax);
    salesInvoice.setGrandTotal(totalTaxable.add(totalTax));

    SalesInvoice savedInvoice = salesInvoiceRepository.save(salesInvoice);

    // --- POSTING LOGIC ---
    
    // 1. Post to GL
    glPostingService.postSalesInvoice(
        savedInvoice.getInvoiceDate(),
        savedInvoice.getId(),
        savedInvoice.getTotalTaxableAmount(),
        savedInvoice.getTotalTaxAmount(),
        savedInvoice.getGrandTotal()
    );

    // 2. Post to Stock Ledger (for each line)
    for (SalesInvoiceLine line : savedInvoice.getLines()) {
      stockPostingService.postStockOut(
          savedInvoice.getInvoiceDate(),
          line.getItem(),
          savedInvoice.getWarehouse(),
          line.getQuantity(),
          StockLedgerEntry.ReferenceType.SALES_INVOICE,
          savedInvoice.getId()
      );
    }

    return mapToDto(savedInvoice);
  }

  public SalesInvoiceDto getSalesInvoice(UUID id) {
    SalesInvoice invoice = salesInvoiceRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    return mapToDto(invoice);
  }

  public List<SalesInvoiceDto> getAllSalesInvoices() {
    return salesInvoiceRepository.findAll().stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
  }

  private SalesInvoiceDto mapToDto(SalesInvoice invoice) {
    List<SalesInvoiceLineDto> lineDtos = invoice.getLines().stream()
        .map(line -> SalesInvoiceLineDto.builder()
            .id(line.getId())
            .itemId(line.getItem().getId())
            .quantity(line.getQuantity())
            .unitPrice(line.getUnitPrice())
            .taxRate(line.getTaxRate())
            .taxAmount(line.getTaxAmount())
            .lineTotal(line.getLineTotal())
            .build())
        .collect(Collectors.toList());

    return SalesInvoiceDto.builder()
        .id(invoice.getId())
        .invoiceNumber(invoice.getInvoiceNumber())
        .invoiceDate(invoice.getInvoiceDate())
        .partyId(invoice.getParty().getId())
        .warehouseId(invoice.getWarehouse().getId())
        .lines(lineDtos)
        .totalTaxableAmount(invoice.getTotalTaxableAmount())
        .totalTaxAmount(invoice.getTotalTaxAmount())
        .grandTotal(invoice.getGrandTotal())
        .build();
  }
}
