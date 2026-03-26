package com.shan.cyber.tech.financetracker.budget.adapter.outbound.crosscontext;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetSpendingTotalsQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class TransactionSpendingAdapter implements SpendingQueryPort {

    private final GetSpendingTotalsQuery spendingTotalsQuery;

    public TransactionSpendingAdapter(GetSpendingTotalsQuery spendingTotalsQuery) {
        this.spendingTotalsQuery = spendingTotalsQuery;
    }

    @Override
    public BigDecimal getSpentAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to) {
        return spendingTotalsQuery.sumExpenses(userId, categoryId, from, to);
    }

    @Override
    public BigDecimal getIncomeAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to) {
        return spendingTotalsQuery.sumIncome(userId, categoryId, from, to);
    }

    @Override
    public Map<CategoryId, BigDecimal> getSpentAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to) {
        return spendingTotalsQuery.sumExpenseBatch(userId, categoryIds, from, to);
    }

    @Override
    public Map<CategoryId, BigDecimal> getIncomeAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to) {
        return spendingTotalsQuery.sumIncomeBatch(userId, categoryIds, from, to);
    }
}
