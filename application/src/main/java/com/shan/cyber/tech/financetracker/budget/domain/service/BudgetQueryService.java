package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

        // Calculate rollover from previous period when rollover_enabled=true
        BigDecimal rolloverAmount = BigDecimal.ZERO;
        if (budget.isRolloverEnabled() && budget.getPeriodType() != BudgetPeriod.CUSTOM) {
            LocalDate prevPeriodEnd = budget.getStartDate().minusDays(1);
            LocalDate prevPeriodStart = calculatePreviousPeriodStart(budget.getPeriodType(), budget.getStartDate());
            BigDecimal previousSpent = spendingQueryPort.getSpentAmount(
                    userId, budget.getCategoryId(), prevPeriodStart, prevPeriodEnd);
            BigDecimal previousRemaining = budgetAmount.subtract(previousSpent);
            // Cap rollover at 100% of the budget amount; minimum 0
            rolloverAmount = previousRemaining.max(BigDecimal.ZERO).min(budgetAmount);
        }

        BigDecimal effectiveBudget = budgetAmount.add(rolloverAmount);
        BigDecimal remaining = effectiveBudget.subtract(spent);

        double percentUsed = effectiveBudget.compareTo(BigDecimal.ZERO) > 0
                ? spent.multiply(BigDecimal.valueOf(100))
                        .divide(effectiveBudget, 2, RoundingMode.HALF_UP)
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
                rolloverAmount.toPlainString(),
                effectiveBudget.toPlainString(),
                budget.getAuditInfo() != null ? budget.getAuditInfo().createdAt() : null);
    }

    private LocalDate calculatePreviousPeriodStart(BudgetPeriod periodType, LocalDate currentPeriodStart) {
        return switch (periodType) {
            case WEEKLY -> currentPeriodStart.minusWeeks(1);
            case BI_WEEKLY -> currentPeriodStart.minusWeeks(2);
            case MONTHLY -> currentPeriodStart.minusMonths(1);
            case QUARTERLY -> currentPeriodStart.minusMonths(3);
            case SEMI_ANNUAL -> currentPeriodStart.minusMonths(6);
            case ANNUALLY -> currentPeriodStart.minusYears(1);
            case CUSTOM -> currentPeriodStart; // not reached
        };
    }
}
