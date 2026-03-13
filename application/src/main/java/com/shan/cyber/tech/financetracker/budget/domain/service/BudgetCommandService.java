package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetCreated;
import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetDeactivated;
import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.DeactivateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public class BudgetCommandService implements CreateBudgetUseCase, UpdateBudgetUseCase, DeactivateBudgetUseCase {

    private final BudgetPersistencePort persistencePort;
    private final BudgetEventPublisherPort eventPublisherPort;

    public BudgetCommandService(BudgetPersistencePort persistencePort,
                                 BudgetEventPublisherPort eventPublisherPort) {
        this.persistencePort = persistencePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public BudgetId createBudget(CreateBudgetCommand command) {
        persistencePort.findActiveByUserAndCategoryAndPeriod(
                command.userId(), command.categoryId(), command.periodType())
                .ifPresent(existing -> { throw new DuplicateActiveBudgetException(); });

        Budget budget = Budget.create(
                command.userId(), command.categoryId(), command.periodType(),
                command.amount(), command.startDate(), command.endDate(),
                command.rolloverEnabled(), command.alertThresholdPct());

        Budget saved = persistencePort.save(budget);

        eventPublisherPort.publish(new BudgetCreated(
                saved.getId(), saved.getUserId(), saved.getCategoryId(), saved.getAmount()));

        return saved.getId();
    }

    @Override
    public void updateBudget(UpdateBudgetCommand command) {
        Budget budget = persistencePort.findById(command.budgetId(), command.userId())
                .orElseThrow(() -> new BudgetNotFoundException(command.budgetId().value()));

        budget.update(command.amount(), command.endDate(),
                command.rolloverEnabled(), command.alertThresholdPct());

        persistencePort.save(budget);
    }

    @Override
    public void deactivateBudget(BudgetId budgetId, UserId userId) {
        Budget budget = persistencePort.findById(budgetId, userId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId.value()));

        budget.deactivate();
        persistencePort.save(budget);

        eventPublisherPort.publish(new BudgetDeactivated(budgetId, userId));
    }
}
