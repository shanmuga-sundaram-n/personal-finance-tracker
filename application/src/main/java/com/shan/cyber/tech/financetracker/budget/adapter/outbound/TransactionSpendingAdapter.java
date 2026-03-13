package com.shan.cyber.tech.financetracker.budget.adapter.outbound;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetSpendingTotalsQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
