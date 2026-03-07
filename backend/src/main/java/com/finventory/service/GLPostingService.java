package com.finventory.service;

import com.finventory.model.GLLine;
import com.finventory.model.GLTransaction;
import com.finventory.repository.GLTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GLPostingService {

  private final GLTransactionRepository glTransactionRepository;

  @Transactional
  public void postSalesInvoice(LocalDate date, UUID invoiceId, BigDecimal totalTaxable, BigDecimal totalTax, BigDecimal grandTotal) {
    GLTransaction transaction = GLTransaction.builder()
        .date(date)
        .refType(GLTransaction.ReferenceType.SALES_INVOICE)
        .refId(invoiceId)
        .description("Sales Invoice Posting")
        .build();

    // Debit Customer (AR)
    GLLine debitAR = GLLine.builder()
        .transaction(transaction)
        .accountHead("ACCOUNTS_RECEIVABLE") // In real app, this would be dynamic based on Party
        .debit(grandTotal)
        .credit(BigDecimal.ZERO)
        .build();

    // Credit Sales Account
    GLLine creditSales = GLLine.builder()
        .transaction(transaction)
        .accountHead("SALES_ACCOUNT")
        .debit(BigDecimal.ZERO)
        .credit(totalTaxable)
        .build();

    // Credit Output Tax
    GLLine creditTax = GLLine.builder()
        .transaction(transaction)
        .accountHead("OUTPUT_TAX") // In real app, split by CGST/SGST/IGST
        .debit(BigDecimal.ZERO)
        .credit(totalTax)
        .build();

    transaction.getLines().add(debitAR);
    transaction.getLines().add(creditSales);
    transaction.getLines().add(creditTax);

    glTransactionRepository.save(transaction);
  }
}
