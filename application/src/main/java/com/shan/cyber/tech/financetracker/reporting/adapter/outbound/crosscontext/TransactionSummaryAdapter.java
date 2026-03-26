package com.shan.cyber.tech.financetracker.reporting.adapter.outbound.crosscontext;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.CategorySpendingSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.RecentTransactionSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.TransactionSummaryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CategorySpending;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetTransactionSummaryQuery;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class TransactionSummaryAdapter implements TransactionSummaryPort {

    private final GetTransactionSummaryQuery transactionSummaryQuery;

    public TransactionSummaryAdapter(GetTransactionSummaryQuery transactionSummaryQuery) {
        this.transactionSummaryQuery = transactionSummaryQuery;
    }

    @Override
    public BigDecimal sumByType(UserId userId, String transactionType, LocalDate from, LocalDate to) {
        return transactionSummaryQuery.sumByType(userId, transactionType, from, to);
    }

    @Override
    public List<CategorySpendingSummary> topExpenseCategories(UserId userId, LocalDate from, LocalDate to, int limit) {
        List<CategorySpending> spendings = transactionSummaryQuery.sumByCategory(userId, from, to, limit);
        return spendings.stream()
                .map(s -> new CategorySpendingSummary(s.categoryId(), s.categoryName(), s.totalAmount()))
                .toList();
    }

    @Override
    public List<RecentTransactionSummary> recentTransactions(UserId userId, int limit) {
        List<TransactionView> views = transactionSummaryQuery.recentTransactions(userId, limit);
        return views.stream()
                .map(v -> new RecentTransactionSummary(
                        v.id(),
                        v.description(),
                        v.amount(),
                        v.currency(),
                        v.type(),
                        v.categoryName(),
                        v.transactionDate().toString()))
                .toList();
    }
}
