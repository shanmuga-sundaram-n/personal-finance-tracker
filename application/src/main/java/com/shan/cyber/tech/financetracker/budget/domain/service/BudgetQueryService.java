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
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<Budget> budgets = persistencePort.findActiveByUser(userId);
        if (budgets.isEmpty()) {
            return List.of();
        }

        // Batch current-period spending: group budgets by (startDate, endDate) and issue
        // one getSpentAmountBatch call per distinct date range instead of one per budget.
        Map<DateRange, List<Budget>> byCurrentRange = budgets.stream()
                .collect(Collectors.groupingBy(b -> new DateRange(b.getStartDate(), b.getEndDate())));

        Map<CategoryId, BigDecimal> currentSpentByCategory = new HashMap<>();
        for (Map.Entry<DateRange, List<Budget>> entry : byCurrentRange.entrySet()) {
            DateRange range = entry.getKey();
            List<CategoryId> categoryIds = entry.getValue().stream()
                    .map(Budget::getCategoryId)
                    .toList();
            Map<CategoryId, BigDecimal> batch = spendingQueryPort.getSpentAmountBatch(
                    userId, categoryIds, range.from(), range.to());
            currentSpentByCategory.putAll(batch);
        }

        // Batch previous-period spending for rollover-enabled budgets only.
        // Group by the computed previous-period (prevStart, prevEnd) and issue one batch per range.
        List<Budget> rolloverBudgets = budgets.stream()
                .filter(b -> b.isRolloverEnabled() && b.getPeriodType() != BudgetPeriod.CUSTOM)
                .toList();

        Map<DateRange, List<Budget>> byPrevRange = rolloverBudgets.stream()
                .collect(Collectors.groupingBy(b -> {
                    LocalDate prevEnd = b.getStartDate().minusDays(1);
                    LocalDate prevStart = calculatePreviousPeriodStart(b.getPeriodType(), b.getStartDate());
                    return new DateRange(prevStart, prevEnd);
                }));

        Map<CategoryId, BigDecimal> prevSpentByCategory = new HashMap<>();
        for (Map.Entry<DateRange, List<Budget>> entry : byPrevRange.entrySet()) {
            DateRange range = entry.getKey();
            List<CategoryId> categoryIds = entry.getValue().stream()
                    .map(Budget::getCategoryId)
                    .toList();
            Map<CategoryId, BigDecimal> batch = spendingQueryPort.getSpentAmountBatch(
                    userId, categoryIds, range.from(), range.to());
            prevSpentByCategory.putAll(batch);
        }

        return budgets.stream()
                .map(budget -> buildView(budget, userId, currentSpentByCategory, prevSpentByCategory))
                .toList();
    }

    @Override
    public BudgetView getById(BudgetId budgetId, UserId userId) {
        Budget budget = persistencePort.findById(budgetId, userId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId.value()));

        // Single-budget path: use individual queries (no batching benefit for one item).
        BigDecimal spent = spendingQueryPort.getSpentAmount(
                userId, budget.getCategoryId(), budget.getStartDate(), budget.getEndDate());

        BigDecimal budgetAmount = budget.getAmount().amount();
        BigDecimal rolloverAmount = BigDecimal.ZERO;
        if (budget.isRolloverEnabled() && budget.getPeriodType() != BudgetPeriod.CUSTOM) {
            LocalDate prevPeriodEnd = budget.getStartDate().minusDays(1);
            LocalDate prevPeriodStart = calculatePreviousPeriodStart(budget.getPeriodType(), budget.getStartDate());
            BigDecimal previousSpent = spendingQueryPort.getSpentAmount(
                    userId, budget.getCategoryId(), prevPeriodStart, prevPeriodEnd);
            BigDecimal previousRemaining = budgetAmount.subtract(previousSpent);
            rolloverAmount = previousRemaining.max(BigDecimal.ZERO).min(budgetAmount);
        }

        return assembleBudgetView(budget, userId, spent, rolloverAmount);
    }

    private BudgetView buildView(Budget budget, UserId userId,
                                  Map<CategoryId, BigDecimal> currentSpentByCategory,
                                  Map<CategoryId, BigDecimal> prevSpentByCategory) {
        BigDecimal spent = currentSpentByCategory.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
        BigDecimal budgetAmount = budget.getAmount().amount();

        BigDecimal rolloverAmount = BigDecimal.ZERO;
        if (budget.isRolloverEnabled() && budget.getPeriodType() != BudgetPeriod.CUSTOM) {
            BigDecimal previousSpent = prevSpentByCategory.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
            BigDecimal previousRemaining = budgetAmount.subtract(previousSpent);
            // Cap rollover at 100% of the budget amount; minimum 0
            rolloverAmount = previousRemaining.max(BigDecimal.ZERO).min(budgetAmount);
        }

        return assembleBudgetView(budget, userId, spent, rolloverAmount);
    }

    private BudgetView assembleBudgetView(Budget budget, UserId userId,
                                           BigDecimal spent, BigDecimal rolloverAmount) {
        String categoryName = categoryNameQueryPort.getCategoryName(budget.getCategoryId(), userId);
        BigDecimal budgetAmount = budget.getAmount().amount();
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

    /** Value type used as a grouping key for date ranges. */
    private record DateRange(LocalDate from, LocalDate to) {}
}
