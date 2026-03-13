package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;

import java.time.LocalDate;

public record CreateTransactionCommand(
        UserId userId,
        AccountId accountId,
        CategoryId categoryId,
        Money amount,
        TransactionType type,
        LocalDate transactionDate,
        String description,
        String merchantName,
        String referenceNumber) {
}
