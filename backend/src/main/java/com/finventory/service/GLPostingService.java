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
  @SuppressWarnings("checkstyle:ParameterNumber")
  public void postSalesInvoice(LocalDate date, UUID invoiceId, com.finventory.model.Party party,
      BigDecimal totalTaxable, BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalIgst,
      BigDecimal grandTotal) {
    GLTransaction transaction = GLTransaction.builder()
        .date(date)
        .refType(GLTransaction.ReferenceType.SALES_INVOICE)
        .refId(invoiceId)
        .party(party)
        .description("Sales Invoice Posting - " + party.getName())
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

  @Transactional
  @SuppressWarnings("checkstyle:ParameterNumber")
  public void postPurchaseInvoice(LocalDate date, UUID invoiceId, com.finventory.model.Party party,
      BigDecimal totalTaxable, BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalIgst,
      BigDecimal grandTotal) {
    GLTransaction transaction = GLTransaction.builder()
        .date(date)
        .refType(GLTransaction.ReferenceType.PURCHASE_INVOICE)
        .refId(invoiceId)
        .party(party)
        .description("Purchase Invoice Posting - " + party.getName())
        .build();

    // Credit Accounts Payable (Liability)
    GLLine creditAP = GLLine.builder()
        .transaction(transaction)
        .accountHead("ACCOUNTS_PAYABLE")
        .debit(BigDecimal.ZERO)
        .credit(grandTotal)
        .build();
    transaction.getLines().add(creditAP);

    // Debit Purchase Account (Expense)
    GLLine debitPurchase = GLLine.builder()
        .transaction(transaction)
        .accountHead("PURCHASE_ACCOUNT")
        .debit(totalTaxable)
        .credit(BigDecimal.ZERO)
        .build();
    transaction.getLines().add(debitPurchase);

    // Debit Input Tax - CGST
    if (totalCgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine debitCgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("INPUT_CGST")
          .debit(totalCgst)
          .credit(BigDecimal.ZERO)
          .build();
      transaction.getLines().add(debitCgst);
    }

    // Debit Input Tax - SGST
    if (totalSgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine debitSgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("INPUT_SGST")
          .debit(totalSgst)
          .credit(BigDecimal.ZERO)
          .build();
      transaction.getLines().add(debitSgst);
    }

    // Debit Input Tax - IGST
    if (totalIgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine debitIgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("INPUT_IGST")
          .debit(totalIgst)
          .credit(BigDecimal.ZERO)
          .build();
      transaction.getLines().add(debitIgst);
    }

    glTransactionRepository.save(transaction);
  }

  @Transactional
  @SuppressWarnings("checkstyle:ParameterNumber")
  public void postSalesReturn(LocalDate date, UUID returnId, com.finventory.model.Party party,
      BigDecimal totalTaxable, BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalIgst,
      BigDecimal grandTotal) {
    GLTransaction transaction = GLTransaction.builder()
        .date(date)
        .refType(GLTransaction.ReferenceType.SALES_RETURN)
        .refId(returnId)
        .party(party)
        .description("Sales Return Posting - " + party.getName())
        .build();

    // Credit Customer (AR) - Reduce Asset
    GLLine creditAR = GLLine.builder()
        .transaction(transaction)
        .accountHead("ACCOUNTS_RECEIVABLE")
        .debit(BigDecimal.ZERO)
        .credit(grandTotal)
        .build();

    // Debit Sales Account (or Sales Return Account) - Reduce Income
    GLLine debitSales = GLLine.builder()
        .transaction(transaction)
        .accountHead("SALES_RETURN_ACCOUNT")
        .debit(totalTaxable)
        .credit(BigDecimal.ZERO)
        .build();

    transaction.getLines().add(creditAR);
    transaction.getLines().add(debitSales);

    // Debit Output Tax - CGST (Reduce Liability)
    if (totalCgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine debitCgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("OUTPUT_CGST")
          .debit(totalCgst)
          .credit(BigDecimal.ZERO)
          .build();
      transaction.getLines().add(debitCgst);
    }

    // Debit Output Tax - SGST (Reduce Liability)
    if (totalSgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine debitSgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("OUTPUT_SGST")
          .debit(totalSgst)
          .credit(BigDecimal.ZERO)
          .build();
      transaction.getLines().add(debitSgst);
    }

    // Debit Output Tax - IGST (Reduce Liability)
    if (totalIgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine debitIgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("OUTPUT_IGST")
          .debit(totalIgst)
          .credit(BigDecimal.ZERO)
          .build();
      transaction.getLines().add(debitIgst);
    }

    glTransactionRepository.save(transaction);
  }

  @Transactional
  @SuppressWarnings("checkstyle:ParameterNumber")
  public void postPurchaseReturn(LocalDate date, UUID returnId, com.finventory.model.Party party,
      BigDecimal totalTaxable, BigDecimal totalCgst, BigDecimal totalSgst, BigDecimal totalIgst,
      BigDecimal grandTotal) {
    GLTransaction transaction = GLTransaction.builder()
        .date(date)
        .refType(GLTransaction.ReferenceType.PURCHASE_RETURN)
        .refId(returnId)
        .party(party)
        .description("Purchase Return Posting - " + party.getName())
        .build();

    // Debit Accounts Payable (Vendor) - Reduce Liability
    GLLine debitAP = GLLine.builder()
        .transaction(transaction)
        .accountHead("ACCOUNTS_PAYABLE")
        .debit(grandTotal)
        .credit(BigDecimal.ZERO)
        .build();

    // Credit Purchase Account (or Purchase Return Account) - Reduce Expense
    GLLine creditPurchase = GLLine.builder()
        .transaction(transaction)
        .accountHead("PURCHASE_RETURN_ACCOUNT")
        .debit(BigDecimal.ZERO)
        .credit(totalTaxable)
        .build();

    transaction.getLines().add(debitAP);
    transaction.getLines().add(creditPurchase);

    // Credit Input Tax - CGST (Reduce Asset/Claim)
    if (totalCgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine creditCgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("INPUT_CGST")
          .debit(BigDecimal.ZERO)
          .credit(totalCgst)
          .build();
      transaction.getLines().add(creditCgst);
    }

    // Credit Input Tax - SGST (Reduce Asset/Claim)
    if (totalSgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine creditSgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("INPUT_SGST")
          .debit(BigDecimal.ZERO)
          .credit(totalSgst)
          .build();
      transaction.getLines().add(creditSgst);
    }

    // Credit Input Tax - IGST (Reduce Asset/Claim)
    if (totalIgst.compareTo(BigDecimal.ZERO) > 0) {
      GLLine creditIgst = GLLine.builder()
          .transaction(transaction)
          .accountHead("INPUT_IGST")
          .debit(BigDecimal.ZERO)
          .credit(totalIgst)
          .build();
      transaction.getLines().add(creditIgst);
    }

    glTransactionRepository.save(transaction);
  }
}
