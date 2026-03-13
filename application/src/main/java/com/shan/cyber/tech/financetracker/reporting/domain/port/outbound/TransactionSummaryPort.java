package com.shan.cyber.tech.financetracker.reporting.domain.port.outbound;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.CategorySpendingSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.RecentTransactionSummary;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionSummaryPort {

    BigDecimal sumByType(UserId userId, String transactionType, LocalDate from, LocalDate to);

    List<CategorySpendingSummary> topExpenseCategories(UserId userId, LocalDate from, LocalDate to, int limit);

    List<RecentTransactionSummary> recentTransactions(UserId userId, int limit);
}
