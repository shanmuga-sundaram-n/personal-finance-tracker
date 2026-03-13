package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GetTransactionSummaryQuery {

    BigDecimal sumByType(UserId userId, String transactionType, LocalDate from, LocalDate to);

    List<CategorySpending> sumByCategory(UserId userId, LocalDate from, LocalDate to, int limit);

    List<TransactionView> recentTransactions(UserId userId, int limit);
}
