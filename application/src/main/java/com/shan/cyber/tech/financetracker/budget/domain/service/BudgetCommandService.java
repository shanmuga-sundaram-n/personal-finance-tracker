package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetCreated;
import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetDeactivated;
import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpsertBudgetByCategoryCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategorySummary;
import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public class BudgetCommandService {

    private final BudgetPersistencePort persistencePort;
    private final BudgetEventPublisherPort eventPublisherPort;
    private final CategoryNameQueryPort categoryNameQueryPort;

    public BudgetCommandService(BudgetPersistencePort persistencePort,
                                 BudgetEventPublisherPort eventPublisherPort,
                                 CategoryNameQueryPort categoryNameQueryPort) {
        this.persistencePort = persistencePort;
        this.eventPublisherPort = eventPublisherPort;
        this.categoryNameQueryPort = categoryNameQueryPort;
    }

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

    public void updateBudget(UpdateBudgetCommand command) {
        Budget budget = persistencePort.findById(command.budgetId(), command.userId())
                .orElseThrow(() -> new BudgetNotFoundException(command.budgetId().value()));

        budget.update(command.amount(), command.endDate(),
                command.rolloverEnabled(), command.alertThresholdPct());

        persistencePort.save(budget);
    }

    public void deactivateBudget(BudgetId budgetId, UserId userId) {
        Budget budget = persistencePort.findById(budgetId, userId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId.value()));

        budget.deactivate();
        persistencePort.save(budget);

        eventPublisherPort.publish(new BudgetDeactivated(budgetId, userId));
    }

    public BudgetId upsertBudget(UpsertBudgetByCategoryCommand command) {
        // Validate: TRANSFER type categories are not budgetable
        List<CategorySummary> allCategories = categoryNameQueryPort.getCategoriesVisibleToUser(command.userId());

        CategorySummary targetCategory = allCategories.stream()
                .filter(c -> c.id().value().equals(command.categoryId().value()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("CATEGORY_NOT_FOUND", "Category not found"));

        if ("TRANSFER".equals(targetCategory.typeCode())) {
            throw new BusinessRuleException("TRANSFER_CATEGORY_NOT_BUDGETABLE",
                    "Transfer categories cannot have budgets");
        }

        // Validate: category must not be a parent (check if any other category has this as its parent)
        boolean isParent = allCategories.stream()
                .anyMatch(c -> command.categoryId().value().equals(c.parentCategoryId()));
        if (isParent) {
            throw new BusinessRuleException("PARENT_CATEGORY_BUDGET_NOT_ALLOWED",
                    "Cannot set a budget on a parent category. Set budgets on child categories instead.");
        }

        // Find existing active budget for this category (any period)
        List<Budget> activeBudgets = persistencePort.findActiveByUserAndDateRange(
                command.userId(), command.startDate(), command.endDate());

        Optional<Budget> existingOpt = activeBudgets.stream()
                .filter(b -> b.getCategoryId().value().equals(command.categoryId().value()))
                .findFirst();

        if (existingOpt.isPresent()) {
            Budget existing = existingOpt.get();
            if (existing.getPeriodType() == command.periodType()) {
                // Same period type: update amount and end date only
                existing.update(command.amount(), command.endDate(),
                        existing.isRolloverEnabled(), existing.getAlertThresholdPct());
                Budget saved = persistencePort.save(existing);
                return saved.getId();
            } else {
                // Different period type: deactivate old, create new
                existing.deactivate();
                persistencePort.save(existing);
                eventPublisherPort.publish(new BudgetDeactivated(existing.getId(), command.userId()));
            }
        }

        // Create new budget
        Budget newBudget = Budget.create(
                command.userId(),
                command.categoryId(),
                command.periodType(),
                command.amount(),
                command.startDate(),
                command.endDate(),
                false,
                null);

        Budget saved = persistencePort.save(newBudget);

        eventPublisherPort.publish(new BudgetCreated(
                saved.getId(), saved.getUserId(), saved.getCategoryId(), saved.getAmount()));

        return saved.getId();
    }
}
