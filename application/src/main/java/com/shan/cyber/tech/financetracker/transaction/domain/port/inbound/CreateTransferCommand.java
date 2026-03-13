package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.LocalDate;

public record CreateTransferCommand(
        UserId userId,
        AccountId fromAccountId,
        AccountId toAccountId,
        CategoryId categoryId,
        Money amount,
        LocalDate transactionDate,
        String description) {
}
