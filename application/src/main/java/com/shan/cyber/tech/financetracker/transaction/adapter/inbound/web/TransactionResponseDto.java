package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransactionResponseDto(
        Long id,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        String amount,
        String currency,
        String type,
        LocalDate transactionDate,
        String description,
        String merchantName,
        String referenceNumber,
        Long transferPairId,
        boolean isRecurring,
        boolean isReconciled,
        OffsetDateTime createdAt) {
}
