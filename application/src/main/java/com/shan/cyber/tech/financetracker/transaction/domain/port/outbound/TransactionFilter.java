package com.shan.cyber.tech.financetracker.transaction.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;

import java.time.LocalDate;

public record TransactionFilter(
        UserId userId,
        AccountId accountId,
        CategoryId categoryId,
        TransactionType type,
        LocalDate fromDate,
        LocalDate toDate) {
}
