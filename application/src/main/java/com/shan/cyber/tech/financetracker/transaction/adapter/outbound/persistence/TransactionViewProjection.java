package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface TransactionViewProjection {
    Long getId();
    Long getAccountId();
    String getAccountName();
    Long getCategoryId();
    String getCategoryName();
    BigDecimal getAmount();
    String getCurrency();
    String getTransactionType();
    LocalDate getTransactionDate();
    String getDescription();
    String getMerchantName();
    String getReferenceNumber();
    Long getTransferPairId();
    boolean getIsRecurring();
    boolean getIsReconciled();
    OffsetDateTime getCreatedAt();
}
