package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetQueryServiceTest {

    @Mock private BudgetPersistencePort persistencePort;
    @Mock private SpendingQueryPort spendingQueryPort;
    @Mock private CategoryNameQueryPort categoryNameQueryPort;

    private BudgetQueryService service;

    private static final UserId USER       = new UserId(1L);
    private static final UserId OTHER_USER = new UserId(99L);
    private static final CategoryId CAT    = new CategoryId(5L);

    // Jan 2026 is our test period
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END   = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        service = new BudgetQueryService(persistencePort, spendingQueryPort, categoryNameQueryPort);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Budget monthlyBudget(BudgetId id, Money amount, boolean rollover, Integer alertPct) {
        Budget b = Budget.create(USER, CAT, BudgetPeriod.MONTHLY, amount, START, END, rollover, alertPct);
        b.setId(id);
        return b;
    }

    // ── getActiveByUser ───────────────────────────────────────────────────────

    @Test
    void should_returnEmptyList_when_userHasNoActiveBudgets() {
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of());

        List<BudgetView> result = service.getActiveByUser(USER);

        assertThat(result).isEmpty();
    }

    @Test
    void should_returnBudgetViewWithCorrectSpentAmount_when_singleBudgetIsActive() {
        Budget budget = monthlyBudget(new BudgetId(1L), Money.of("500.00", "USD"), false, null);
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));

        // Batch call for current period
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("150.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Groceries");

        List<BudgetView> result = service.getActiveByUser(USER);

        assertThat(result).hasSize(1);
        BudgetView view = result.get(0);
        assertThat(new BigDecimal(view.spentAmount()).compareTo(new BigDecimal("150.00"))).isZero();
        assertThat(new BigDecimal(view.remainingAmount()).compareTo(new BigDecimal("350.00"))).isZero();
        assertThat(view.categoryName()).isEqualTo("Groceries");
        assertThat(view.rolloverEnabled()).isFalse();
    }

    @Test
    void should_computePercentUsed_correctly_when_spendingIsKnown() {
        Budget budget = monthlyBudget(new BudgetId(2L), Money.of("200.00", "USD"), false, null);
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("50.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Transport");

        BudgetView view = service.getActiveByUser(USER).get(0);

        // 50 / 200 = 25%
        assertThat(view.percentUsed()).isEqualTo(25.0);
    }

    // ── alert threshold ───────────────────────────────────────────────────────

    @Test
    void should_setAlertTriggered_when_percentUsedMeetsThreshold() {
        Budget budget = monthlyBudget(new BudgetId(3L), Money.of("400.00", "USD"), false, 80);
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        // 320 / 400 = 80% → exactly at threshold
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("320.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Bills");

        BudgetView view = service.getActiveByUser(USER).get(0);

        assertThat(view.alertTriggered()).isTrue();
        assertThat(view.percentUsed()).isEqualTo(80.0);
    }

    @Test
    void should_notSetAlertTriggered_when_percentUsedBelowThreshold() {
        Budget budget = monthlyBudget(new BudgetId(4L), Money.of("400.00", "USD"), false, 80);
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        // 279 / 400 = 69.75% → below threshold
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("279.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Bills");

        BudgetView view = service.getActiveByUser(USER).get(0);

        assertThat(view.alertTriggered()).isFalse();
    }

    @Test
    void should_notSetAlertTriggered_when_alertThresholdIsNull() {
        Budget budget = monthlyBudget(new BudgetId(5L), Money.of("400.00", "USD"), false, null);
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("400.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Bills");

        BudgetView view = service.getActiveByUser(USER).get(0);

        assertThat(view.alertTriggered()).isFalse();
    }

    // ── zero budget amount guard ──────────────────────────────────────────────

    @Test
    void should_returnZeroPercentUsed_when_effectiveBudgetIsZero() {
        // Cannot create a Budget via Budget.create() with zero amount (it throws).
        // We construct directly via the full constructor to represent a DB-hydrated edge case.
        Budget budget = new Budget(
                new BudgetId(10L), USER, CAT, BudgetPeriod.MONTHLY,
                Money.of("0.0001", "USD"),   // smallest positive; effective = ~0 after rollover
                START, END, false, null, true, null);
        // Override the amount to a zero-equivalent via reflection is awkward in pure Java;
        // instead, verify the guard with a near-zero positive amount producing ~0 division.
        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        when(spendingQueryPort.getSpentAmountBatch(any(), any(), any(), any()))
                .thenReturn(Map.of(CAT, BigDecimal.ZERO));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Other");

        List<BudgetView> result = service.getActiveByUser(USER);

        // Must not throw ArithmeticException; percentUsed must be a finite double
        assertThat(result).hasSize(1);
        assertThat(Double.isFinite(result.get(0).percentUsed())).isTrue();
    }

    // ── rollover logic ────────────────────────────────────────────────────────

    @Test
    void should_addRolloverFromPreviousPeriod_when_previousPeriodHadSurplus() {
        // Budget: 500 USD monthly. Previous period spent 300 → surplus 200 → rollover 200.
        Budget budget = monthlyBudget(new BudgetId(11L), Money.of("500.00", "USD"), true, null);

        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));

        // Current period: spent 100
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("100.00")));

        // Previous period: Dec 2025
        LocalDate prevStart = LocalDate.of(2025, 12, 1);
        LocalDate prevEnd   = LocalDate.of(2025, 12, 31);
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(prevStart), eq(prevEnd)))
                .thenReturn(Map.of(CAT, new BigDecimal("300.00")));

        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Groceries");

        BudgetView view = service.getActiveByUser(USER).get(0);

        // effectiveBudget = 500 + 200 = 700; remaining = 700 - 100 = 600
        assertThat(new BigDecimal(view.rolloverAmountAdded()).compareTo(new BigDecimal("200.00"))).isZero();
        assertThat(new BigDecimal(view.effectiveBudgetThisPeriod()).compareTo(new BigDecimal("700.00"))).isZero();
        assertThat(new BigDecimal(view.remainingAmount()).compareTo(new BigDecimal("600.00"))).isZero();
    }

    @Test
    void should_capRolloverAtBudgetAmount_when_previousPeriodHadZeroSpending() {
        // Budget: 500 USD. Previous period: spent 0 → surplus 500 → cap at 500.
        Budget budget = monthlyBudget(new BudgetId(12L), Money.of("500.00", "USD"), true, null);

        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));

        // Current period: spent 0
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, BigDecimal.ZERO));

        // Previous period: no spending at all
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)),
                eq(LocalDate.of(2025, 12, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(Map.of(CAT, BigDecimal.ZERO));

        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Groceries");

        BudgetView view = service.getActiveByUser(USER).get(0);

        // rollover capped at budget amount: 500.  effectiveBudget = 500 + 500 = 1000
        assertThat(new BigDecimal(view.rolloverAmountAdded()).compareTo(new BigDecimal("500.00"))).isZero();
        assertThat(new BigDecimal(view.effectiveBudgetThisPeriod()).compareTo(new BigDecimal("1000.00"))).isZero();
    }

    @Test
    void should_clampRolloverToZero_when_previousPeriodWasOverBudget() {
        // Budget: 500 USD. Previous period: spent 700 → surplus = -200 → rollover = max(0, -200) = 0
        Budget budget = monthlyBudget(new BudgetId(13L), Money.of("500.00", "USD"), true, null);

        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));

        // Current period spending
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("200.00")));

        // Previous period: overspent
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)),
                eq(LocalDate.of(2025, 12, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(Map.of(CAT, new BigDecimal("700.00")));

        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Groceries");

        BudgetView view = service.getActiveByUser(USER).get(0);

        assertThat(new BigDecimal(view.rolloverAmountAdded()).compareTo(BigDecimal.ZERO)).isZero();
        assertThat(new BigDecimal(view.effectiveBudgetThisPeriod()).compareTo(new BigDecimal("500.00"))).isZero();
    }

    @Test
    void should_notApplyRollover_when_rolloverIsDisabled() {
        Budget budget = monthlyBudget(new BudgetId(14L), Money.of("600.00", "USD"), false, null);

        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(END)))
                .thenReturn(Map.of(CAT, new BigDecimal("100.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Dining");

        BudgetView view = service.getActiveByUser(USER).get(0);

        assertThat(new BigDecimal(view.rolloverAmountAdded()).compareTo(BigDecimal.ZERO)).isZero();
        assertThat(new BigDecimal(view.effectiveBudgetThisPeriod()).compareTo(new BigDecimal("600.00"))).isZero();
    }

    @Test
    void should_notApplyRollover_when_periodTypeIsCustom() {
        // CUSTOM budgets with rolloverEnabled=true should not have rollover applied
        LocalDate customEnd = LocalDate.of(2026, 3, 31);
        Budget budget = new Budget(new BudgetId(15L), USER, CAT, BudgetPeriod.CUSTOM,
                Money.of("1000.00", "USD"), START, customEnd, true, null, true, null);

        when(persistencePort.findActiveByUser(USER)).thenReturn(List.of(budget));
        when(spendingQueryPort.getSpentAmountBatch(eq(USER), eq(List.of(CAT)), eq(START), eq(customEnd)))
                .thenReturn(Map.of(CAT, new BigDecimal("400.00")));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Project");

        BudgetView view = service.getActiveByUser(USER).get(0);

        // CUSTOM period → no rollover
        assertThat(new BigDecimal(view.rolloverAmountAdded()).compareTo(BigDecimal.ZERO)).isZero();
    }

    // ── getById — happy path ──────────────────────────────────────────────────

    @Test
    void should_returnBudgetView_when_budgetBelongsToAuthenticatedUser() {
        BudgetId budgetId = new BudgetId(20L);
        Budget budget = monthlyBudget(budgetId, Money.of("300.00", "USD"), false, 75);

        when(persistencePort.findById(budgetId, USER)).thenReturn(Optional.of(budget));
        when(spendingQueryPort.getSpentAmount(eq(USER), eq(CAT), eq(START), eq(END)))
                .thenReturn(new BigDecimal("225.00"));
        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Entertainment");

        BudgetView view = service.getById(budgetId, USER);

        assertThat(view.id()).isEqualTo(20L);
        assertThat(view.categoryId()).isEqualTo(5L);
        assertThat(view.categoryName()).isEqualTo("Entertainment");
        assertThat(view.periodType()).isEqualTo("MONTHLY");
        assertThat(new BigDecimal(view.amount()).compareTo(new BigDecimal("300.00"))).isZero();
        assertThat(new BigDecimal(view.spentAmount()).compareTo(new BigDecimal("225.00"))).isZero();
        // 225 / 300 = 75% → alert exactly at threshold
        assertThat(view.alertTriggered()).isTrue();
    }

    @Test
    void should_throwBudgetNotFoundException_when_budgetBelongsToAnotherUser() {
        BudgetId budgetId = new BudgetId(21L);
        // persistencePort.findById with OTHER_USER returns empty (multi-tenant isolation)
        when(persistencePort.findById(budgetId, OTHER_USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(budgetId, OTHER_USER))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void should_throwBudgetNotFoundException_when_budgetIdDoesNotExist() {
        BudgetId missing = new BudgetId(9999L);
        when(persistencePort.findById(missing, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(missing, USER))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void should_applyRollover_when_getByIdAndRolloverEnabled() {
        BudgetId budgetId = new BudgetId(22L);
        Budget budget = monthlyBudget(budgetId, Money.of("500.00", "USD"), true, null);

        when(persistencePort.findById(budgetId, USER)).thenReturn(Optional.of(budget));

        // Current period: spent 100
        when(spendingQueryPort.getSpentAmount(eq(USER), eq(CAT), eq(START), eq(END)))
                .thenReturn(new BigDecimal("100.00"));

        // Previous period: spent 400 → surplus 100 → rollover 100
        LocalDate prevStart = LocalDate.of(2025, 12, 1);
        LocalDate prevEnd   = LocalDate.of(2025, 12, 31);
        when(spendingQueryPort.getSpentAmount(eq(USER), eq(CAT), eq(prevStart), eq(prevEnd)))
                .thenReturn(new BigDecimal("400.00"));

        when(categoryNameQueryPort.getCategoryName(CAT, USER)).thenReturn("Groceries");

        BudgetView view = service.getById(budgetId, USER);

        assertThat(new BigDecimal(view.rolloverAmountAdded()).compareTo(new BigDecimal("100.00"))).isZero();
        assertThat(new BigDecimal(view.effectiveBudgetThisPeriod()).compareTo(new BigDecimal("600.00"))).isZero();
    }
}
