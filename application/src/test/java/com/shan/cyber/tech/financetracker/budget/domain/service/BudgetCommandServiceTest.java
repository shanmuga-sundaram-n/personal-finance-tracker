package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpsertBudgetByCategoryCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategorySummary;
import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetCommandServiceTest {

    @Mock private BudgetPersistencePort persistencePort;
    @Mock private BudgetEventPublisherPort eventPublisherPort;
    @Mock private CategoryNameQueryPort categoryNameQueryPort;

    private BudgetCommandService service;

    private static final UserId USER = new UserId(1L);
    private static final CategoryId CATEGORY = new CategoryId(5L);
    private static final Money AMOUNT = Money.of("500.00", "USD");
    private static final LocalDate START = LocalDate.of(2025, 1, 1);
    private static final LocalDate END   = LocalDate.of(2025, 1, 31);

    @BeforeEach
    void setUp() {
        service = new BudgetCommandService(persistencePort, eventPublisherPort, categoryNameQueryPort);
    }

    @Test
    void createBudget_success_returnsBudgetId() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, null, false, 80);

        Budget saved = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, null, false, 80);
        saved.setId(new BudgetId(1L));

        when(persistencePort.findActiveByUserAndCategoryAndPeriod(USER, CATEGORY, BudgetPeriod.MONTHLY))
                .thenReturn(Optional.empty());
        when(persistencePort.save(any(Budget.class))).thenReturn(saved);

        BudgetId result = service.createBudget(command);

        assertNotNull(result);
        assertEquals(1L, result.value());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void createBudget_duplicateActiveBudget_throwsDuplicateActiveBudgetException() {
        CreateBudgetCommand command = new CreateBudgetCommand(
                USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, null, false, null);

        Budget existing = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, null, false, null);
        existing.setId(new BudgetId(99L));

        when(persistencePort.findActiveByUserAndCategoryAndPeriod(USER, CATEGORY, BudgetPeriod.MONTHLY))
                .thenReturn(Optional.of(existing));

        assertThrows(DuplicateActiveBudgetException.class, () -> service.createBudget(command));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void updateBudget_success_savesUpdatedBudget() {
        BudgetId budgetId = new BudgetId(1L);
        Budget budget = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, null, false, null);
        budget.setId(budgetId);

        Money newAmount = Money.of("800.00", "USD");
        UpdateBudgetCommand command = new UpdateBudgetCommand(budgetId, USER, newAmount, null, true, 90);

        when(persistencePort.findById(budgetId, USER)).thenReturn(Optional.of(budget));

        service.updateBudget(command);

        verify(persistencePort).save(budget);
        assertEquals(newAmount, budget.getAmount());
        assertTrue(budget.isRolloverEnabled());
        assertEquals(90, budget.getAlertThresholdPct());
    }

    @Test
    void updateBudget_notFound_throwsBudgetNotFoundException() {
        BudgetId budgetId = new BudgetId(999L);
        UpdateBudgetCommand command = new UpdateBudgetCommand(budgetId, USER, AMOUNT, null, false, null);

        when(persistencePort.findById(budgetId, USER)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> service.updateBudget(command));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void deactivateBudget_success_deactivatesAndPublishesEvent() {
        BudgetId budgetId = new BudgetId(1L);
        Budget budget = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, null, false, null);
        budget.setId(budgetId);

        when(persistencePort.findById(budgetId, USER)).thenReturn(Optional.of(budget));

        service.deactivateBudget(budgetId, USER);

        assertFalse(budget.isActive());
        verify(persistencePort).save(budget);
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void deactivateBudget_notFound_throwsBudgetNotFoundException() {
        BudgetId budgetId = new BudgetId(999L);
        when(persistencePort.findById(budgetId, USER)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> service.deactivateBudget(budgetId, USER));
        verify(persistencePort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    // ── Upsert tests ─────────────────────────────────────────────────────────

    @Test
    void upsertBudget_noBudgetExists_creates() {
        // Arrange: category visible, no existing budget for the period range
        CategorySummary leafCategory = new CategorySummary(CATEGORY, "Groceries", "EXPENSE", null);
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER)).thenReturn(List.of(leafCategory));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of());

        Budget created = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, END, false, null);
        created.setId(new BudgetId(10L));
        when(persistencePort.save(any(Budget.class))).thenReturn(created);

        UpsertBudgetByCategoryCommand command = new UpsertBudgetByCategoryCommand(
                USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, END);

        // Act
        BudgetId result = service.upsertBudget(command);

        // Assert: save called once, event published once, returned id matches
        verify(persistencePort, times(1)).save(any(Budget.class));
        verify(eventPublisherPort, times(1)).publish(any());
        assertEquals(10L, result.value());
    }

    @Test
    void upsertBudget_samePeriodType_updatesAmount() {
        // Arrange: existing MONTHLY budget; command is also MONTHLY with a new amount
        CategorySummary leafCategory = new CategorySummary(CATEGORY, "Groceries", "EXPENSE", null);
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER)).thenReturn(List.of(leafCategory));

        Budget existing = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, END, false, null);
        existing.setId(new BudgetId(5L));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of(existing));
        when(persistencePort.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Money newAmount = Money.of("800.00", "USD");
        UpsertBudgetByCategoryCommand command = new UpsertBudgetByCategoryCommand(
                USER, CATEGORY, BudgetPeriod.MONTHLY, newAmount, START, END);

        // Act
        BudgetId result = service.upsertBudget(command);

        // Assert: existing budget id returned, no deactivation event, amount updated
        assertEquals(5L, result.value());
        assertEquals(newAmount, existing.getAmount());
        // Only one save (the update), no deactivate event
        verify(persistencePort, times(1)).save(any(Budget.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void upsertBudget_differentPeriodType_deactivatesAndCreates() {
        // Arrange: existing MONTHLY budget; command switches to WEEKLY
        CategorySummary leafCategory = new CategorySummary(CATEGORY, "Groceries", "EXPENSE", null);
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER)).thenReturn(List.of(leafCategory));

        Budget existing = Budget.create(USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, END, false, null);
        existing.setId(new BudgetId(5L));
        when(persistencePort.findActiveByUserAndDateRange(USER, START, END)).thenReturn(List.of(existing));

        Budget newBudget = Budget.create(USER, CATEGORY, BudgetPeriod.WEEKLY, AMOUNT, START, END, false, null);
        newBudget.setId(new BudgetId(6L));
        // First save = deactivate old (returns modified existing), second save = create new
        when(persistencePort.save(any(Budget.class)))
                .thenAnswer(inv -> {
                    Budget b = inv.getArgument(0);
                    return b.getId() != null && Long.valueOf(5L).equals(b.getId().value()) ? b : newBudget;
                });

        UpsertBudgetByCategoryCommand command = new UpsertBudgetByCategoryCommand(
                USER, CATEGORY, BudgetPeriod.WEEKLY, AMOUNT, START, END);

        // Act
        BudgetId result = service.upsertBudget(command);

        // Assert: old budget deactivated, new budget created, events for both
        assertFalse(existing.isActive(), "Old budget must be deactivated");
        assertEquals(6L, result.value());
        // Two saves: one for deactivation, one for creation
        verify(persistencePort, times(2)).save(any(Budget.class));
        // Two events: BudgetDeactivated + BudgetCreated
        verify(eventPublisherPort, times(2)).publish(any());
    }

    @Test
    void upsertBudget_parentCategory_throws422() {
        // Arrange: target category has a child (making it a parent) → must be rejected
        CategoryId childId = new CategoryId(50L);
        CategorySummary targetCategory  = new CategorySummary(CATEGORY, "Housing", "EXPENSE", null);
        CategorySummary childCategory   = new CategorySummary(childId, "Rent", "EXPENSE", CATEGORY.value());
        when(categoryNameQueryPort.getCategoriesVisibleToUser(USER))
                .thenReturn(List.of(targetCategory, childCategory));

        UpsertBudgetByCategoryCommand command = new UpsertBudgetByCategoryCommand(
                USER, CATEGORY, BudgetPeriod.MONTHLY, AMOUNT, START, END);

        // Act & Assert
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.upsertBudget(command));

        assertEquals("PARENT_CATEGORY_BUDGET_NOT_ALLOWED", ex.getErrorCode());
        verify(persistencePort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
