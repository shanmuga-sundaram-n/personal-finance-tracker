package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BudgetQueryService implements GetBudgetsQuery {

    private final BudgetPersistencePort persistencePort;
    private final SpendingQueryPort spendingQueryPort;
    private final CategoryNameQueryPort categoryNameQueryPort;

    public BudgetQueryService(BudgetPersistencePort persistencePort,
                               SpendingQueryPort spendingQueryPort,
                               CategoryNameQueryPort categoryNameQueryPort) {
        this.persistencePort = persistencePort;
        this.spendingQueryPort = spendingQueryPort;
        this.categoryNameQueryPort = categoryNameQueryPort;
    }

    @Override
    public List<BudgetView> getActiveByUser(UserId userId) {
        return persistencePort.findActiveByUser(userId).stream()
                .map(budget -> buildView(budget, userId))
                .toList();
    }

    @Override
    public BudgetView getById(BudgetId budgetId, UserId userId) {
        Budget budget = persistencePort.findById(budgetId, userId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId.value()));
        return buildView(budget, userId);
    }

    private BudgetView buildView(Budget budget, UserId userId) {
        String categoryName = categoryNameQueryPort.getCategoryName(budget.getCategoryId(), userId);

        BigDecimal spent = spendingQueryPort.getSpentAmount(
                userId, budget.getCategoryId(), budget.getStartDate(), budget.getEndDate());

        BigDecimal budgetAmount = budget.getAmount().amount();
        BigDecimal remaining = budgetAmount.subtract(spent);

        double percentUsed = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? spent.multiply(BigDecimal.valueOf(100))
                        .divide(budgetAmount, 2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;

        boolean alertTriggered = budget.getAlertThresholdPct() != null
                && percentUsed >= budget.getAlertThresholdPct();

        return new BudgetView(
                budget.getId().value(),
                budget.getCategoryId().value(),
                categoryName,
                budget.getPeriodType().name(),
                budgetAmount.toPlainString(),
                budget.getAmount().currency(),
                budget.getStartDate(),
                budget.getEndDate(),
                budget.isRolloverEnabled(),
                budget.getAlertThresholdPct(),
                budget.isActive(),
                spent.toPlainString(),
                remaining.toPlainString(),
                percentUsed,
                alertTriggered,
                budget.getAuditInfo() != null ? budget.getAuditInfo().createdAt() : null);
    }
}
