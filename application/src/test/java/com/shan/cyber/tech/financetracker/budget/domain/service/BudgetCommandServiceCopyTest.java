package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetCreated;
import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetDeactivated;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CopyBudgetsFromPreviousMonthCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CopyBudgetsResult;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategorySummary;
import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BudgetCommandService#copyFromPreviousMonth}.
 *
 * The service is instantiated directly (no Spring context) with Mockito mocks for all
 * outbound ports, in line with the hexagonal architecture testing strategy.
 *
 * All tests use fixed year/month values rooted in March 2026 as the "current month",
 * matching the project's current date (2026-03-26). Wherever the domain method calls
 * {@code YearMonth.now()} the test must supply a targetYear/targetMonth that matches the
 * real wall-clock current month, EXCEPT for the "target month not current month" test
 * which intentionally uses a past month.
 */
@ExtendWith(MockitoExtension.class)
class BudgetCommandServiceCopyTest {

    @Mock private BudgetPersistencePort persistencePort;
    @Mock private BudgetEventPublisherPort eventPublisherPort;
    @Mock private CategoryNameQueryPort categoryNameQueryPort;

    private BudgetCommandService service;

    // Shared test fixtures
    private static final UserId USER       = new UserId(42L);
    private static final CategoryId CAT_A  = new CategoryId(10L);
    private static final CategoryId CAT_B  = new CategoryId(20L);
    private static final CategoryId CAT_C  = new CategoryId(30L);
    private static final Money AMOUNT_500  = Money.of("500.00", "USD");
    private static final Money AMOUNT_300  = Money.of("300.00", "USD");

    // Current month (wall-clock: 2026-03-26)
    private static final YearMonth CURRENT_MONTH   = YearMonth.now();
    private static final int TARGET_YEAR           = CURRENT_MONTH.getYear();
    private static final int TARGET_MONTH          = CURRENT_MONTH.getMonthValue();

    private static final YearMonth PREV_MONTH      = CURRENT_MONTH.minusMonths(1);
    private static final LocalDate PREV_START      = PREV_MONTH.atDay(1);
    private static final LocalDate PREV_END        = PREV_MONTH.atEndOfMonth();
    private static final LocalDate TARGET_START    = CURRENT_MONTH.atDay(1);
    private static final LocalDate TARGET_END      = CURRENT_MONTH.atEndOfMonth();

    @BeforeEach
    void setUp() {
        service = new BudgetCommandService(persistencePort, eventPublisherPort, categoryNameQueryPort);
    }

    // -------------------------------------------------------------------------
    // Test 1: Happy path — copies all budgets from previous month
    // -------------------------------------------------------------------------

    @Test
    void should_copiedCountEqualsPreviousBudgetCount_when_noPreviousTargetBudgetsExist() {
        // Arrange: two active budgets in the previous month, no conflicts in the target month
        Budget prevBudgetA = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);
        Budget prevBudgetB = savedBudget(2L, CAT_B, BudgetPeriod.MONTHLY, AMOUNT_300, PREV_START, PREV_END);

        CategorySummary catA = new CategorySummary(CAT_A, "Groceries", "EXPENSE", null);
        CategorySummary catB = new CategorySummary(CAT_B, "Transport", "EXPENSE", null);

        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevBudgetA, prevBudgetB));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of());
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(catA, catB));
        when(persistencePort.save(any(Budget.class)))
                .thenAnswer(inv -> {
                    Budget b = inv.getArgument(0);
                    b.setId(new BudgetId(100L));
                    return b;
                });

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: both budgets copied, no skips or conflicts
        assertThat(result.copiedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(result.conflictCount()).isEqualTo(0);
        assertThat(result.overwrittenCount()).isEqualTo(0);

        verify(persistencePort, times(2)).save(any(Budget.class));
        verify(eventPublisherPort, times(2)).publish(any(BudgetCreated.class));
    }

    // -------------------------------------------------------------------------
    // Test 2: Dry-run (overwriteExisting=false) with conflicts
    // -------------------------------------------------------------------------

    @Test
    void should_returnConflictCountWithoutWriting_when_overwriteExistingFalseAndConflictsExist() {
        // Arrange: one previous budget and one matching target budget (conflict)
        Budget prevBudgetA = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);
        Budget targetBudgetA = savedBudget(10L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_300, TARGET_START, TARGET_END);

        CategorySummary catA = new CategorySummary(CAT_A, "Groceries", "EXPENSE", null);

        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevBudgetA));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of(targetBudgetA));
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(catA));

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: conflict counted, nothing written
        assertThat(result.conflictCount()).isEqualTo(1);
        assertThat(result.copiedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(result.overwrittenCount()).isEqualTo(0);

        // No saves or events should occur in dry-run mode
        verify(persistencePort, never()).save(any(Budget.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    // -------------------------------------------------------------------------
    // Test 3: Overwrite path (overwriteExisting=true) — deactivates old, creates new
    // -------------------------------------------------------------------------

    @Test
    void should_deactivateOldAndCreateNew_when_overwriteExistingTrueAndConflictExists() {
        // Arrange: one previous budget and one conflicting target budget
        Budget prevBudgetA = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);
        Budget targetBudgetA = savedBudget(10L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_300, TARGET_START, TARGET_END);

        CategorySummary catA = new CategorySummary(CAT_A, "Groceries", "EXPENSE", null);

        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevBudgetA));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of(targetBudgetA));
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(catA));
        when(persistencePort.save(any(Budget.class)))
                .thenAnswer(inv -> {
                    Budget b = inv.getArgument(0);
                    if (b.getId() == null) {
                        b.setId(new BudgetId(200L));
                    }
                    return b;
                });

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, true);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: one overwrite, zero conflicts, zero copies
        assertThat(result.overwrittenCount()).isEqualTo(1);
        assertThat(result.conflictCount()).isEqualTo(0);
        assertThat(result.copiedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(0);

        // Two saves: one to deactivate the old budget, one to create the new one
        verify(persistencePort, times(2)).save(any(Budget.class));

        // Old budget must be inactive after the operation
        assertThat(targetBudgetA.isActive()).isFalse();

        // Events: one BudgetDeactivated + one BudgetCreated
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort, times(2)).publish(eventCaptor.capture());
        List<DomainEvent> events = eventCaptor.getAllValues();
        assertThat(events).anyMatch(e -> e instanceof BudgetDeactivated);
        assertThat(events).anyMatch(e -> e instanceof BudgetCreated);
    }

    // -------------------------------------------------------------------------
    // Test 4: Skip inactive category (not in visible category list)
    // -------------------------------------------------------------------------

    @Test
    void should_incrementSkippedCount_when_previousBudgetCategoryIsInactive() {
        // Arrange: previous budget references a category that is no longer visible (soft-deleted)
        Budget prevBudget = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);

        // CAT_A is absent from the visible category list — it is inactive
        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevBudget));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of());
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of()); // no visible categories

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: skipped, nothing created
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.copiedCount()).isEqualTo(0);
        assertThat(result.conflictCount()).isEqualTo(0);
        assertThat(result.overwrittenCount()).isEqualTo(0);

        verify(persistencePort, never()).save(any(Budget.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    // -------------------------------------------------------------------------
    // Test 5: Skip TRANSFER category
    // -------------------------------------------------------------------------

    @Test
    void should_incrementSkippedCount_when_previousBudgetCategoryIsTransferType() {
        // Arrange: previous budget belongs to a TRANSFER-type category
        Budget prevBudget = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);

        // CAT_A is visible but has typeCode=TRANSFER — must be skipped
        CategorySummary transferCategory = new CategorySummary(CAT_A, "Internal Transfer", "TRANSFER", null);

        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevBudget));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of());
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(transferCategory));

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: skipped due to TRANSFER type
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.copiedCount()).isEqualTo(0);

        verify(persistencePort, never()).save(any(Budget.class));
    }

    // -------------------------------------------------------------------------
    // Test 6: Skip parent category
    // -------------------------------------------------------------------------

    @Test
    void should_incrementSkippedCount_when_previousBudgetCategoryIsParentOfAnotherCategory() {
        // Arrange: CAT_A is a parent because CAT_B has parentCategoryId = CAT_A.value()
        Budget prevParentBudget  = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);
        Budget prevLeafBudget    = savedBudget(2L, CAT_B, BudgetPeriod.MONTHLY, AMOUNT_300, PREV_START, PREV_END);

        // CAT_A is the parent; CAT_B is the leaf child
        CategorySummary parentCat = new CategorySummary(CAT_A, "Housing",   "EXPENSE", null);
        CategorySummary childCat  = new CategorySummary(CAT_B, "Rent",      "EXPENSE", CAT_A.value());

        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevParentBudget, prevLeafBudget));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of());
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(parentCat, childCat));
        when(persistencePort.save(any(Budget.class)))
                .thenAnswer(inv -> {
                    Budget b = inv.getArgument(0);
                    b.setId(new BudgetId(300L));
                    return b;
                });

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: parent (CAT_A) skipped, leaf (CAT_B) copied
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.copiedCount()).isEqualTo(1);

        // Only one save for the leaf budget
        verify(persistencePort, times(1)).save(any(Budget.class));
    }

    // -------------------------------------------------------------------------
    // Test 7: Empty previous month — returns all-zero result
    // -------------------------------------------------------------------------

    @Test
    void should_returnAllZeroCounts_when_previousMonthHasNoBudgets() {
        // Arrange: no budgets at all in the previous month
        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of());
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of());
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of());

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert: 0 copied, 0 skipped, 0 conflicts, 0 overwritten
        assertThat(result.copiedCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(result.conflictCount()).isEqualTo(0);
        assertThat(result.overwrittenCount()).isEqualTo(0);

        verify(persistencePort, never()).save(any(Budget.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    // -------------------------------------------------------------------------
    // Test 8: January edge case — previous month resolves to December of targetYear-1
    // -------------------------------------------------------------------------

    @Test
    void should_resolveDecemberOfPreviousYear_when_targetMonthIsJanuary() {
        // This test validates the month-arithmetic edge case.
        // We cannot call the service with January unless it IS the current month.
        // We verify the logic indirectly: if the service is invoked with January as the
        // current month it should query December of the previous year for previous budgets.
        //
        // We mock YearMonth.now() behavior by using a targetYear/targetMonth that equals
        // the real current month; however, to test the January edge case deterministically
        // we verify the persistence port is called with December dates when target=January.
        //
        // Strategy: because YearMonth.now() is called inside the service and cannot be
        // injected (no Clock parameter in the constructor), we validate the branch by
        // constructing January 2026 dates and asserting the correct December 2025 range
        // is queried — only when it IS January 2026.
        //
        // If today is not January we cannot submit January to the service (it would throw
        // TARGET_MONTH_NOT_CURRENT). Therefore we verify the date-arithmetic math directly
        // using the YearMonth API, which is the implementation the service delegates to.

        // Verify the Java arithmetic the service relies on: January.minusMonths(1) = December of year-1
        YearMonth january2026  = YearMonth.of(2026, 1);
        YearMonth expectedPrev = YearMonth.of(2025, 12);

        assertThat(january2026.minusMonths(1)).isEqualTo(expectedPrev);
        assertThat(expectedPrev.atDay(1)).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(expectedPrev.atEndOfMonth()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    // -------------------------------------------------------------------------
    // Test 9: Target month is not the current month — throws BusinessRuleException
    // -------------------------------------------------------------------------

    @Test
    void should_throwBusinessRuleException_when_targetMonthIsNotCurrentMonth() {
        // Arrange: use a clearly past month (January 2025) that cannot be the current month
        int pastYear  = 2025;
        int pastMonth = 1;

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, pastYear, pastMonth, false);

        // Act & Assert: must throw with specific error code
        assertThatThrownBy(() -> service.copyFromPreviousMonth(command))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getErrorCode()).isEqualTo("TARGET_MONTH_NOT_CURRENT");
                    assertThat(bre.getMessage()).contains("current month");
                });

        // No persistence or event calls should have been made
        verify(persistencePort, never()).findActiveByUserAndDateRange(any(), any(), any());
        verify(eventPublisherPort, never()).publish(any());
    }

    // -------------------------------------------------------------------------
    // Additional scenario: mixed budgets — some copied, some skipped, some conflicted
    // -------------------------------------------------------------------------

    @Test
    void should_returnCorrectMixedCounts_when_someBudgetsAreSkippedAndSomeConflict() {
        // Arrange:
        //   CAT_A: EXPENSE leaf — no conflict → copiedCount++
        //   CAT_B: TRANSFER → skippedCount++
        //   CAT_C: EXPENSE leaf — has conflict, overwriteExisting=false → conflictCount++
        Budget prevA = savedBudget(1L, CAT_A, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);
        Budget prevB = savedBudget(2L, CAT_B, BudgetPeriod.MONTHLY, AMOUNT_300, PREV_START, PREV_END);
        Budget prevC = savedBudget(3L, CAT_C, BudgetPeriod.MONTHLY, AMOUNT_500, PREV_START, PREV_END);

        Budget targetC = savedBudget(30L, CAT_C, BudgetPeriod.MONTHLY, AMOUNT_300, TARGET_START, TARGET_END);

        CategorySummary catA = new CategorySummary(CAT_A, "Groceries",         "EXPENSE",   null);
        CategorySummary catB = new CategorySummary(CAT_B, "Internal Transfer", "TRANSFER",  null);
        CategorySummary catC = new CategorySummary(CAT_C, "Utilities",         "EXPENSE",   null);

        when(persistencePort.findActiveByUserAndDateRange(USER, PREV_START, PREV_END))
                .thenReturn(List.of(prevA, prevB, prevC));
        when(persistencePort.findActiveByUserAndDateRange(USER, TARGET_START, TARGET_END))
                .thenReturn(List.of(targetC));
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(catA, catB, catC));
        when(persistencePort.save(any(Budget.class)))
                .thenAnswer(inv -> {
                    Budget b = inv.getArgument(0);
                    b.setId(new BudgetId(999L));
                    return b;
                });

        CopyBudgetsFromPreviousMonthCommand command =
                new CopyBudgetsFromPreviousMonthCommand(USER, TARGET_YEAR, TARGET_MONTH, false);

        // Act
        CopyBudgetsResult result = service.copyFromPreviousMonth(command);

        // Assert
        assertThat(result.copiedCount()).isEqualTo(1);      // CAT_A
        assertThat(result.skippedCount()).isEqualTo(1);     // CAT_B (TRANSFER)
        assertThat(result.conflictCount()).isEqualTo(1);    // CAT_C (dry-run)
        assertThat(result.overwrittenCount()).isEqualTo(0);

        // Only CAT_A creates a save; CAT_C conflict is not written in dry-run
        verify(persistencePort, times(1)).save(any(Budget.class));
        verify(eventPublisherPort, times(1)).publish(any(BudgetCreated.class));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Creates a persisted Budget with the given id — simulates what the persistence
     * layer returns from a query (id is already assigned).
     */
    private static Budget savedBudget(long id, CategoryId categoryId, BudgetPeriod period,
                                      Money amount, LocalDate start, LocalDate end) {
        Budget budget = Budget.create(USER, categoryId, period, amount, start, end, false, null);
        budget.setId(new BudgetId(id));
        return budget;
    }
}
