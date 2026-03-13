package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetCommandServiceTest {

    @Mock private BudgetPersistencePort persistencePort;
    @Mock private BudgetEventPublisherPort eventPublisherPort;

    private BudgetCommandService service;

    private static final UserId USER = new UserId(1L);
    private static final CategoryId CATEGORY = new CategoryId(5L);
    private static final Money AMOUNT = Money.of("500.00", "USD");
    private static final LocalDate START = LocalDate.of(2025, 1, 1);

    @BeforeEach
    void setUp() {
        service = new BudgetCommandService(persistencePort, eventPublisherPort);
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
}
