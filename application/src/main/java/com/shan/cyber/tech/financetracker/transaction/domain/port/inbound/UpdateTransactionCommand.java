package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.LocalDate;

public record UpdateTransactionCommand(
        TransactionId transactionId,
        UserId userId,
        CategoryId categoryId,
        Money amount,
        LocalDate transactionDate,
        String description,
        String merchantName,
        String referenceNumber) {
}
