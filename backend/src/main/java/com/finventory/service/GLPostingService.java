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
  public void postSalesInvoice(LocalDate date, UUID invoiceId, BigDecimal totalTaxable,
      BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalIgst, BigDecimal grandTotal) {
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

    transaction.getLines().add(debitAR);
    transaction.getLines().add(creditSales);

    // Credit Output Tax - CGST
    if (totalCgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine creditCgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("OUTPUT_CGST")
          .debit(BigDecimal.ZERO)
          .credit(totalCgst)
          .build();
      transaction.getLines().add(creditCgst);
    }

    // Credit Output Tax - SGST
    if (totalSgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine creditSgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("OUTPUT_SGST")
          .debit(BigDecimal.ZERO)
          .credit(totalSgst)
          .build();
      transaction.getLines().add(creditSgst);
    }

    // Credit Output Tax - IGST
    if (totalIgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine creditIgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("OUTPUT_IGST")
          .debit(BigDecimal.ZERO)
          .credit(totalIgst)
          .build();
      transaction.getLines().add(creditIgst);
    }

    glTransactionRepository.save(transaction);
  }
}
