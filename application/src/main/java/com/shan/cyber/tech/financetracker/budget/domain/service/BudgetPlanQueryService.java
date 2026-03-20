package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryGroup;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryRow;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanTotals;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetPlanQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategorySummary;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.UserPreferenceQueryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BudgetPlanQueryService implements GetBudgetPlanQuery {

    private final BudgetPersistencePort persistencePort;
    private final SpendingQueryPort spendingQueryPort;
    private final CategoryNameQueryPort categoryNameQueryPort;
    private final UserPreferenceQueryPort userPreferenceQueryPort;

    public BudgetPlanQueryService(BudgetPersistencePort persistencePort,
                                   SpendingQueryPort spendingQueryPort,
                                   CategoryNameQueryPort categoryNameQueryPort,
                                   UserPreferenceQueryPort userPreferenceQueryPort) {
        this.persistencePort = persistencePort;
        this.spendingQueryPort = spendingQueryPort;
        this.categoryNameQueryPort = categoryNameQueryPort;
        this.userPreferenceQueryPort = userPreferenceQueryPort;
    }

    @Override
    public BudgetPlanView getBudgetPlan(UserId userId, LocalDate startDate, LocalDate endDate) {
        List<CategorySummary> allCategories = categoryNameQueryPort.getCategoriesVisibleToUser(userId);
        List<Budget> activeBudgets = persistencePort.findActiveByUserAndDateRange(userId, startDate, endDate);

        // If a category has multiple overlapping budgets, keep the one created most recently
        Map<Long, Budget> budgetByCategoryId = activeBudgets.stream()
                .collect(Collectors.toMap(
                        b -> b.getCategoryId().value(),
                        b -> b,
                        (existing, replacement) -> {
                            if (existing.getAuditInfo() == null || replacement.getAuditInfo() == null) {
                                return existing;
                            }
                            return replacement.getAuditInfo().createdAt()
                                    .isAfter(existing.getAuditInfo().createdAt()) ? replacement : existing;
                        }));

        String currency = userPreferenceQueryPort.getPreferredCurrency(userId);

        List<CategorySummary> incomeCategories = allCategories.stream()
                .filter(c -> "INCOME".equals(c.typeCode()))
                .toList();
        List<CategorySummary> expenseCategories = allCategories.stream()
                .filter(c -> "EXPENSE".equals(c.typeCode()))
                .toList();

        List<CategoryId> allCategoryIds = allCategories.stream()
                .map(CategorySummary::id)
                .toList();

        Map<CategoryId, BigDecimal> spentBatch = spendingQueryPort.getSpentAmountBatch(userId, allCategoryIds, startDate, endDate);
        Map<CategoryId, BigDecimal> incomeBatch = spendingQueryPort.getIncomeAmountBatch(userId, allCategoryIds, startDate, endDate);

        List<BudgetPlanCategoryRow> incomeRows = incomeCategories.stream()
                .map(c -> buildRow(c, budgetByCategoryId, incomeBatch))
                .toList();

        // ── Expense grouping algorithm ──────────────────────────────────────────
        // 1. Compute set of all IDs that appear as a parentCategoryId in the list
        Set<Long> parentIds = allCategories.stream()
                .map(CategorySummary::parentCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> allCategoryIdValues = allCategories.stream()
                .map(c -> c.id().value())
                .collect(Collectors.toSet());

        // 2. Build parent name lookup
        Map<Long, String> parentNameById = allCategories.stream()
                .filter(c -> parentIds.contains(c.id().value()))
                .collect(Collectors.toMap(c -> c.id().value(), CategorySummary::name));

        // 3. Group expense leaf categories by their parentCategoryId
        // A leaf = its parentCategoryId is non-null AND that parent exists in the list
        List<CategorySummary> expenseLeaves = expenseCategories.stream()
                .filter(c -> c.parentCategoryId() != null && allCategoryIdValues.contains(c.parentCategoryId()))
                .toList();

        // Ungrouped leaves = expense categories with no parent OR parent not in list
        // AND they are not themselves a parent
        List<CategorySummary> ungroupedExpenseLeaves = expenseCategories.stream()
                .filter(c -> (c.parentCategoryId() == null || !allCategoryIdValues.contains(c.parentCategoryId()))
                              && !parentIds.contains(c.id().value()))
                .toList();

        Map<Long, List<CategorySummary>> byParent = expenseLeaves.stream()
                .collect(Collectors.groupingBy(CategorySummary::parentCategoryId));

        List<BudgetPlanCategoryGroup> expenseGroups = new ArrayList<>();

        // Grouped: one group per parent, sorted by parent name
        parentIds.stream()
                .filter(allCategoryIdValues::contains)
                .sorted(Comparator.comparing(id -> parentNameById.getOrDefault(id, "")))
                .forEach(parentId -> {
                    List<BudgetPlanCategoryRow> rows = byParent.getOrDefault(parentId, List.of())
                            .stream()
                            .map(c -> buildRow(c, budgetByCategoryId, spentBatch))
                            .toList();
                    BigDecimal monthlyTotal = rows.stream()
                            .map(BudgetPlanCategoryRow::monthlyAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal yearlyTotal = rows.stream()
                            .map(BudgetPlanCategoryRow::yearlyAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal actualTotal = rows.stream()
                            .map(BudgetPlanCategoryRow::actualAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    boolean alert = rows.stream().anyMatch(r -> r.percentUsed() > 100.0);
                    expenseGroups.add(new BudgetPlanCategoryGroup(
                            parentId, parentNameById.get(parentId), rows, monthlyTotal, yearlyTotal, actualTotal, alert));
                });

        // Ungrouped: each gets a solo group
        ungroupedExpenseLeaves.forEach(c -> {
            BudgetPlanCategoryRow row = buildRow(c, budgetByCategoryId, spentBatch);
            expenseGroups.add(new BudgetPlanCategoryGroup(
                    null, c.name(), List.of(row), row.monthlyAmount(), row.yearlyAmount(),
                    row.actualAmount(), row.percentUsed() > 100.0));
        });

        // Flatten all leaf rows from all groups for totals computation
        List<BudgetPlanCategoryRow> allExpenseLeafRows = expenseGroups.stream()
                .flatMap(g -> g.rows().stream())
                .toList();

        BudgetPlanTotals incomeTotals = computeTotals(incomeRows, true);
        BudgetPlanTotals expenseTotals = computeTotals(allExpenseLeafRows, false);

        return new BudgetPlanView(startDate, endDate, currency, incomeRows, expenseGroups, incomeTotals, expenseTotals);
    }

    private BudgetPlanCategoryRow buildRow(CategorySummary category,
                                            Map<Long, Budget> budgetByCategoryId,
                                            Map<CategoryId, BigDecimal> actualAmounts) {
        Budget budget = budgetByCategoryId.get(category.id().value());
        BigDecimal budgetedAmount = budget != null ? budget.getAmount().amount() : BigDecimal.ZERO;
        boolean isIncome = "INCOME".equals(category.typeCode());

        BigDecimal actual = actualAmounts.getOrDefault(category.id(), BigDecimal.ZERO);

        // Income: positive variance = earned more than planned (good)
        // Expense: positive variance = spent less than planned (good)
        BigDecimal variance = isIncome
                ? actual.subtract(budgetedAmount)
                : budgetedAmount.subtract(actual);
        double percentUsed = budgetedAmount.compareTo(BigDecimal.ZERO) > 0
                ? actual.multiply(BigDecimal.valueOf(100))
                        .divide(budgetedAmount, 2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;

        BigDecimal monthlyAmount = budget != null
                ? budget.getAmount().amount()
                        .multiply(BudgetPeriod.valueOf(budget.getPeriodType().name()).toMonthlyMultiplier())
                        .setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal yearlyAmount = budget != null
                ? budget.getAmount().amount()
                        .multiply(BigDecimal.valueOf(12).multiply(BudgetPeriod.valueOf(budget.getPeriodType().name()).toMonthlyMultiplier()))
                        .setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String frequency = budget != null ? budget.getPeriodType().name() : null;

        return new BudgetPlanCategoryRow(
                category.id().value(),
                category.name(),
                budget != null ? budget.getId().value() : null,
                budgetedAmount,
                actual,
                variance,
                percentUsed,
                budget != null,
                frequency,
                monthlyAmount,
                yearlyAmount
        );
    }

    private BudgetPlanTotals computeTotals(List<BudgetPlanCategoryRow> rows, boolean isIncome) {
        // Only rows with a budget set contribute to budgeted/actual/variance — keeps variance meaningful
        List<BudgetPlanCategoryRow> budgetedRows = rows.stream()
                .filter(BudgetPlanCategoryRow::hasBudget)
                .toList();
        BigDecimal totalBudgeted = budgetedRows.stream()
                .map(BudgetPlanCategoryRow::budgetedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActual = budgetedRows.stream()
                .map(BudgetPlanCategoryRow::actualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Mirror row-level sign convention: income positive = surplus, expense positive = under budget
        BigDecimal totalVariance = isIncome
                ? totalActual.subtract(totalBudgeted)
                : totalBudgeted.subtract(totalActual);
        double totalPercentUsed = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalActual.multiply(BigDecimal.valueOf(100))
                        .divide(totalBudgeted, 2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;
        // Monthly/yearly totals sum all leaf rows (including unbudgeted, which contribute 0)
        BigDecimal totalMonthly = rows.stream()
                .map(BudgetPlanCategoryRow::monthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalYearly = rows.stream()
                .map(BudgetPlanCategoryRow::yearlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BudgetPlanTotals(totalBudgeted, totalActual, totalVariance, totalPercentUsed, totalMonthly, totalYearly);
    }
}
