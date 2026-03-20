package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryRow;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryGroup;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategorySummary;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.UserPreferenceQueryPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetPlanQueryServiceTest {

    @Mock private BudgetPersistencePort persistencePort;
    @Mock private SpendingQueryPort spendingQueryPort;
    @Mock private CategoryNameQueryPort categoryNameQueryPort;
    @Mock private UserPreferenceQueryPort userPreferenceQueryPort;

    private BudgetPlanQueryService service;

    static final UserId USER = new UserId(1L);
    static final LocalDate START = LocalDate.of(2025, 3, 1);
    static final LocalDate END   = LocalDate.of(2025, 3, 31);

    static final CategoryId SALARY_ID        = new CategoryId(1L);
    static final CategoryId FREELANCE_ID     = new CategoryId(2L);
    static final CategoryId GROCERIES_ID     = new CategoryId(10L);
    static final CategoryId RENT_ID          = new CategoryId(11L);
    static final CategoryId ENTERTAINMENT_ID = new CategoryId(12L);

    // Hierarchy test IDs
    static final CategoryId PARENT_ID  = new CategoryId(100L);
    static final CategoryId CHILD1_ID  = new CategoryId(10L);   // re-uses GROCERIES value
    static final CategoryId CHILD2_ID  = new CategoryId(11L);   // re-uses RENT value

    @BeforeEach void setUp() {
        when(userPreferenceQueryPort.getPreferredCurrency(USER)).thenReturn("USD");
        service = new BudgetPlanQueryService(persistencePort, spendingQueryPort, categoryNameQueryPort, userPreferenceQueryPort);
    }

    // -- Happy path -----------------------------------------------------------

    @Test
    void mixedCategories_returnsCorrectRowCounts() {
        givenCategories(income(SALARY_ID), income(FREELANCE_ID),
                        expense(GROCERIES_ID), expense(RENT_ID), expense(ENTERTAINMENT_ID));
        givenBudgets(budget(10L, SALARY_ID, "3000"), budget(20L, GROCERIES_ID, "600"), budget(21L, RENT_ID, "1200"));
        givenIncome(Map.of(SALARY_ID, bd("2500"), FREELANCE_ID, bd("400")));
        givenSpent(Map.of(GROCERIES_ID, bd("480"), RENT_ID, bd("1200"), ENTERTAINMENT_ID, bd("75")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        assertEquals(2, view.incomeRows().size());
        assertEquals(3, expenseRows(view).size());
    }

    @Test
    void budgetedAmounts_matchMockedBudgets() {
        givenCategories(income(SALARY_ID), expense(GROCERIES_ID));
        givenBudgets(budget(10L, SALARY_ID, "3000"), budget(20L, GROCERIES_ID, "600"));
        givenIncome(Map.of(SALARY_ID, bd("2500")));
        givenSpent(Map.of(GROCERIES_ID, bd("480")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        assertEquals(0, bd("3000").compareTo(row(view.incomeRows(), SALARY_ID).budgetedAmount()));
        assertEquals(0, bd("600").compareTo(row(expenseRows(view), GROCERIES_ID).budgetedAmount()));
    }

    @Test
    void actualAmounts_comeFromBatchSpendingPort() {
        givenCategories(income(SALARY_ID), expense(GROCERIES_ID));
        givenBudgets(budget(10L, SALARY_ID, "3000"), budget(20L, GROCERIES_ID, "600"));
        givenIncome(Map.of(SALARY_ID, bd("2500")));
        givenSpent(Map.of(GROCERIES_ID, bd("480")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        assertEquals(0, bd("2500").compareTo(row(view.incomeRows(), SALARY_ID).actualAmount()));
        assertEquals(0, bd("480").compareTo(row(expenseRows(view), GROCERIES_ID).actualAmount()));
    }

    // -- Variance sign conventions --------------------------------------------

    @Test
    void expenseUnderBudget_varianceIsPositive() {
        // budget - actual = 600 - 480 = +120 (good, under budget)
        givenCategories(expense(GROCERIES_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"));
        givenSpent(Map.of(GROCERIES_ID, bd("480")));

        assertEquals(0, bd("120").compareTo(expenseRows(service.getBudgetPlan(USER, START, END)).get(0).varianceAmount()));
    }

    @Test
    void expenseOverBudget_varianceIsNegative() {
        // budget - actual = 600 - 720 = -120 (bad, over budget)
        givenCategories(expense(GROCERIES_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"));
        givenSpent(Map.of(GROCERIES_ID, bd("720")));

        BudgetPlanCategoryRow row = expenseRows(service.getBudgetPlan(USER, START, END)).get(0);
        assertEquals(0, bd("-120").compareTo(row.varianceAmount()));
        assertTrue(row.varianceAmount().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void incomeSurplus_varianceIsPositive() {
        // actual - budget = 3500 - 3000 = +500 (good, earned more than planned)
        givenCategories(income(SALARY_ID));
        givenBudgets(budget(10L, SALARY_ID, "3000"));
        givenIncome(Map.of(SALARY_ID, bd("3500")));

        BudgetPlanCategoryRow row = service.getBudgetPlan(USER, START, END).incomeRows().get(0);
        assertEquals(0, bd("500").compareTo(row.varianceAmount()));
        assertTrue(row.percentUsed() > 100.0);
    }

    @Test
    void incomeShortfall_varianceIsNegative() {
        // actual - budget = 2500 - 3000 = -500 (bad, earned less than planned)
        givenCategories(income(SALARY_ID));
        givenBudgets(budget(10L, SALARY_ID, "3000"));
        givenIncome(Map.of(SALARY_ID, bd("2500")));

        assertEquals(0, bd("-500").compareTo(service.getBudgetPlan(USER, START, END).incomeRows().get(0).varianceAmount()));
    }

    // -- Percent used ---------------------------------------------------------

    @Test
    void percentUsed_calculatedCorrectly() {
        givenCategories(expense(GROCERIES_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"));
        givenSpent(Map.of(GROCERIES_ID, bd("480")));
        assertEquals(80.00, expenseRows(service.getBudgetPlan(USER, START, END)).get(0).percentUsed(), 0.01);
    }

    @Test
    void fullySpent_percentUsedIs100() {
        givenCategories(expense(RENT_ID));
        givenBudgets(budget(21L, RENT_ID, "1200"));
        givenSpent(Map.of(RENT_ID, bd("1200")));
        assertEquals(100.0, expenseRows(service.getBudgetPlan(USER, START, END)).get(0).percentUsed(), 0.01);
    }

    @Test
    void overBudget_percentUsedExceeds100() {
        givenCategories(expense(GROCERIES_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"));
        givenSpent(Map.of(GROCERIES_ID, bd("720")));
        assertEquals(120.00, expenseRows(service.getBudgetPlan(USER, START, END)).get(0).percentUsed(), 0.01);
    }

    @Test
    void zeroBudgetAmount_percentUsedIsZeroNoDivisionByZero() {
        givenCategories(expense(GROCERIES_ID));
        Budget zeroBudget = new Budget(new BudgetId(99L), USER, GROCERIES_ID, BudgetPeriod.MONTHLY,
                Money.of(BigDecimal.ZERO, "USD"), START, END, false, null, true, null);
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of(zeroBudget));
        givenSpent(Map.of(GROCERIES_ID, bd("50")));
        BudgetPlanView view = assertDoesNotThrow(() -> service.getBudgetPlan(USER, START, END));
        assertEquals(0.0, expenseRows(view).get(0).percentUsed(), 0.001);
    }

    // -- Unbudgeted categories ------------------------------------------------

    @Test
    void categoryWithoutBudget_hasBudgetFalseNullId() {
        givenCategories(income(FREELANCE_ID));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of());
        givenIncome(Map.of(FREELANCE_ID, bd("400")));

        BudgetPlanCategoryRow row = service.getBudgetPlan(USER, START, END).incomeRows().get(0);
        assertFalse(row.hasBudget());
        assertEquals(0, BigDecimal.ZERO.compareTo(row.budgetedAmount()));
        assertNull(row.budgetId());
    }

    @Test
    void noBudgets_allRowsHaveBudgetFalseZeroPercent() {
        givenCategories(income(SALARY_ID), expense(GROCERIES_ID));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of());
        givenIncome(Map.of(SALARY_ID, bd("1000")));
        givenSpent(Map.of(GROCERIES_ID, bd("200")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        view.incomeRows().forEach(r -> { assertFalse(r.hasBudget()); assertEquals(0.0, r.percentUsed(), 0.001); });
        expenseRows(view).forEach(r -> { assertFalse(r.hasBudget()); assertEquals(0.0, r.percentUsed(), 0.001); });
        assertEquals(0, BigDecimal.ZERO.compareTo(view.expenseTotals().totalBudgeted()));
    }

    @Test
    void noCategories_returnsEmptyListsAndZeroTotals() {
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER)).thenReturn(List.of());
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of());
        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        assertTrue(view.incomeRows().isEmpty());
        assertTrue(view.expenseGroups().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(view.incomeTotals().totalBudgeted()));
        assertEquals(0, BigDecimal.ZERO.compareTo(view.expenseTotals().totalBudgeted()));
    }

    // -- Totals ---------------------------------------------------------------

    @Test
    void totals_onlyBudgetedRowsContribute() {
        // Entertainment unbudgeted (actual 75) — excluded from totals
        // Groceries budgeted 600 (actual 480) — included
        givenCategories(expense(GROCERIES_ID), expense(ENTERTAINMENT_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"));
        givenSpent(Map.of(GROCERIES_ID, bd("480"), ENTERTAINMENT_ID, bd("75")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        assertEquals(0, bd("600").compareTo(view.expenseTotals().totalBudgeted()));
        assertEquals(0, bd("480").compareTo(view.expenseTotals().totalActual()));
    }

    @Test
    void expenseTotals_percentUsedReflectsAggregate() {
        // 1680 / 1800 = 93.33%
        givenCategories(expense(GROCERIES_ID), expense(RENT_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"), budget(21L, RENT_ID, "1200"));
        givenSpent(Map.of(GROCERIES_ID, bd("480"), RENT_ID, bd("1200")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);
        assertEquals(0, bd("1800").compareTo(view.expenseTotals().totalBudgeted()));
        assertEquals(0, bd("1680").compareTo(view.expenseTotals().totalActual()));
        assertEquals(93.33, view.expenseTotals().totalPercentUsed(), 0.01);
    }

    // -- Currency -------------------------------------------------------------

    @Test
    void currency_takenFromUserPreference() {
        // Currency now comes from UserPreferenceQueryPort, not from budget amounts
        when(userPreferenceQueryPort.getPreferredCurrency(USER)).thenReturn("EUR");
        givenCategories(expense(GROCERIES_ID));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of());
        givenSpent(Map.of());
        assertEquals("EUR", service.getBudgetPlan(USER, START, END).currency());
    }

    @Test
    void currency_defaultsToUsdWhenPreferenceNotSet() {
        when(userPreferenceQueryPort.getPreferredCurrency(USER)).thenReturn("USD");
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER)).thenReturn(List.of());
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of());
        assertEquals("USD", service.getBudgetPlan(USER, START, END).currency());
    }

    // -- DSL helpers ----------------------------------------------------------

    private void givenCategories(CategorySummary... cs) {
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER)).thenReturn(List.of(cs));
    }
    private void givenBudgets(Budget... bs) {
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of(bs));
    }
    private void givenIncome(Map<CategoryId, BigDecimal> map) {
        when(spendingQueryPort.getIncomeAmountBatch(eq(USER), anyList(), eq(START), eq(END))).thenReturn(map);
    }
    private void givenSpent(Map<CategoryId, BigDecimal> map) {
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), anyList(), eq(START), eq(END))).thenReturn(map);
    }
    private CategorySummary income(CategoryId id)  { return new CategorySummary(id, id.value().toString(), "INCOME", null); }
    private CategorySummary expense(CategoryId id) { return new CategorySummary(id, id.value().toString(), "EXPENSE", null); }
    /** Expense leaf with an explicit parent. */
    private CategorySummary expenseChild(CategoryId id, Long parentId) {
        return new CategorySummary(id, id.value().toString(), "EXPENSE", parentId);
    }
    /** Parent category node (no parent itself). */
    private CategorySummary expenseParent(CategoryId id) {
        return new CategorySummary(id, "Parent-" + id.value(), "EXPENSE", null);
    }
    private Budget budget(long bid, CategoryId cid, String amt) {
        return new Budget(new BudgetId(bid), USER, cid, BudgetPeriod.MONTHLY, Money.of(amt, "USD"), START, END, false, null, true, null);
    }
    private Budget weeklyBudget(long bid, CategoryId cid, String amt) {
        return new Budget(new BudgetId(bid), USER, cid, BudgetPeriod.WEEKLY, Money.of(amt, "USD"), START, END, false, null, true, null);
    }
    private BigDecimal bd(String v) { return new BigDecimal(v); }
    /** Flatten all rows from all expense groups — used by pre-hierarchy tests. */
    private List<BudgetPlanCategoryRow> expenseRows(BudgetPlanView view) {
        return view.expenseGroups().stream()
                .flatMap(g -> g.rows().stream())
                .toList();
    }
    private BudgetPlanCategoryRow row(List<BudgetPlanCategoryRow> rows, CategoryId id) {
        return rows.stream().filter(r -> id.value().equals(r.categoryId())).findFirst()
                .orElseThrow(() -> new AssertionError("No row for categoryId=" + id.value()));
    }

    // ── New: expense-group hierarchy tests ───────────────────────────────────

    @Test
    void groupedCategories_expensesGroupedByParent() {
        // Parent P=100, children C1=10 and C2=11
        CategoryId P = new CategoryId(100L);
        CategoryId C1 = new CategoryId(10L);
        CategoryId C2 = new CategoryId(11L);

        givenCategories(expenseParent(P), expenseChild(C1, 100L), expenseChild(C2, 100L));
        givenBudgets(budget(20L, C1, "600"), budget(21L, C2, "1200"));
        givenSpent(Map.of(C1, bd("0"), C2, bd("0")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);

        // Exactly one group with parentCategoryId = 100, two rows in it
        assertEquals(1, view.expenseGroups().size());
        BudgetPlanCategoryGroup group = view.expenseGroups().get(0);
        assertEquals(100L, group.parentCategoryId());
        assertEquals(2, group.rows().size());
    }

    @Test
    void ungroupedLeaf_getsOwnGroup() {
        // A leaf with no parent gets its own solo group (parentCategoryId = null)
        givenCategories(expense(GROCERIES_ID));
        givenBudgets(budget(20L, GROCERIES_ID, "600"));
        givenSpent(Map.of(GROCERIES_ID, bd("300")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);

        assertEquals(1, view.expenseGroups().size());
        BudgetPlanCategoryGroup group = view.expenseGroups().get(0);
        assertNull(group.parentCategoryId(), "Ungrouped leaf should have null parentCategoryId");
        assertEquals(1, group.rows().size());
    }

    @Test
    void parentCategoryItself_notInChildRows() {
        // Parent P itself must NOT appear as a row in any group's rows list
        CategoryId P = new CategoryId(100L);
        CategoryId C1 = new CategoryId(10L);

        givenCategories(expenseParent(P), expenseChild(C1, 100L));
        givenBudgets(budget(20L, C1, "600"));
        givenSpent(Map.of(C1, bd("0")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);

        List<Long> allRowCategoryIds = view.expenseGroups().stream()
                .flatMap(g -> g.rows().stream())
                .map(BudgetPlanCategoryRow::categoryId)
                .toList();

        assertFalse(allRowCategoryIds.contains(100L),
                "Parent category id 100 must not appear as a row in any group");
    }

    @Test
    void groupMonthlyTotal_sumOfChildrenMonthlyAmounts() {
        // C1 MONTHLY 600, C2 MONTHLY 1200 → groupMonthlyTotal = 1800 (multiplier = 1 for MONTHLY)
        CategoryId P = new CategoryId(100L);
        CategoryId C1 = new CategoryId(10L);
        CategoryId C2 = new CategoryId(11L);

        givenCategories(expenseParent(P), expenseChild(C1, 100L), expenseChild(C2, 100L));
        givenBudgets(budget(20L, C1, "600"), budget(21L, C2, "1200"));
        givenSpent(Map.of(C1, bd("0"), C2, bd("0")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);

        assertEquals(1, view.expenseGroups().size());
        BigDecimal monthlyTotal = view.expenseGroups().get(0).groupMonthlyTotal();
        // MONTHLY multiplier = 1, so 600 + 1200 = 1800
        assertEquals(0, bd("1800").compareTo(monthlyTotal),
                "groupMonthlyTotal should be 1800 but was " + monthlyTotal);
    }

    @Test
    void monthlyAmount_calculatedFromFrequencyMultiplier() {
        // WEEKLY budget of 100 → monthlyAmount = 100 × (13/3) ≈ 433.33
        givenCategories(expense(GROCERIES_ID));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END))
                .thenReturn(List.of(weeklyBudget(20L, GROCERIES_ID, "100")));
        givenSpent(Map.of(GROCERIES_ID, bd("0")));

        BudgetPlanView view = service.getBudgetPlan(USER, START, END);

        List<BudgetPlanCategoryRow> rows = expenseRows(view);
        assertEquals(1, rows.size());
        double monthly = rows.get(0).monthlyAmount().doubleValue();
        assertEquals(433.33, monthly, 0.01,
                "WEEKLY 100 should produce monthlyAmount ≈ 433.33 but was " + monthly);
    }
}
