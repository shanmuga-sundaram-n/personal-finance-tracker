package com.shan.cyber.tech.financetracker.budget.domain.service;

import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetCreated;
import com.shan.cyber.tech.financetracker.budget.domain.event.BudgetDeactivated;
import com.shan.cyber.tech.financetracker.budget.domain.exception.BudgetNotFoundException;
import com.shan.cyber.tech.financetracker.budget.domain.exception.DuplicateActiveBudgetException;
import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CopyBudgetsFromPreviousMonthCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CopyBudgetsResult;
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
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    public CopyBudgetsResult copyFromPreviousMonth(CopyBudgetsFromPreviousMonthCommand command) {
        // Validate target month is current month
        YearMonth now = YearMonth.now();
        YearMonth target = YearMonth.of(command.targetYear(), command.targetMonth());
        if (!target.equals(now)) {
            throw new BusinessRuleException("TARGET_MONTH_NOT_CURRENT",
                    "Budget copy is only allowed for the current month");
        }

        // Compute previous month
        YearMonth prevMonth = target.minusMonths(1);
        LocalDate prevStart = prevMonth.atDay(1);
        LocalDate prevEnd = prevMonth.atEndOfMonth();
        LocalDate targetStart = target.atDay(1);
        LocalDate targetEnd = target.atEndOfMonth();

        // Bulk-fetch budgets for both months
        List<Budget> previousBudgets = persistencePort.findActiveByUserAndDateRange(
                command.userId(), prevStart, prevEnd);
        List<Budget> targetBudgets = persistencePort.findActiveByUserAndDateRange(
                command.userId(), targetStart, targetEnd);

        // Fetch all categories visible to user (single query)
        List<CategorySummary> allCategories = categoryNameQueryPort.getCategoriesVisibleToUser(command.userId());

        // Build lookup maps
        Map<Long, CategorySummary> categoryById = new HashMap<>();
        for (CategorySummary cat : allCategories) {
            categoryById.put(cat.id().value(), cat);
        }
        // NOTE: parentCategoryIds uses raw Long (not CategoryId typed-ID) because
        // CategorySummary.parentCategoryId() returns Long — keep this consistent if
        // CategorySummary is ever refactored to return CategoryId.
        Set<Long> parentCategoryIds = new HashSet<>();
        for (CategorySummary cat : allCategories) {
            if (cat.parentCategoryId() != null) {
                parentCategoryIds.add(cat.parentCategoryId());
            }
        }

        // Build map of existing target-month budgets by categoryId for quick lookup
        Map<Long, Budget> targetBudgetByCategory = new HashMap<>();
        for (Budget b : targetBudgets) {
            targetBudgetByCategory.put(b.getCategoryId().value(), b);
        }

        int copiedCount = 0;
        int skippedCount = 0;
        int conflictCount = 0;
        int overwrittenCount = 0;

        for (Budget source : previousBudgets) {
            Long catId = source.getCategoryId().value();
            CategorySummary cat = categoryById.get(catId);

            // Skip: inactive category (not in visible list), TRANSFER type, or parent category
            if (cat == null || "TRANSFER".equals(cat.typeCode()) || parentCategoryIds.contains(catId)) {
                skippedCount++;
                continue;
            }

            Budget existing = targetBudgetByCategory.get(catId);
            if (existing != null) {
                // Conflict: target month already has a budget for this category
                if (!command.overwriteExisting()) {
                    conflictCount++;
                } else {
                    // Soft-delete the existing target-month budget
                    existing.deactivate();
                    persistencePort.save(existing);
                    eventPublisherPort.publish(new BudgetDeactivated(existing.getId(), command.userId()));

                    // Create new budget copying source properties
                    Budget copy = Budget.create(
                            command.userId(),
                            source.getCategoryId(),
                            source.getPeriodType(),
                            source.getAmount(),
                            targetStart,
                            source.getPeriodType() != BudgetPeriod.CUSTOM
                                    ? source.getPeriodType().calculateEndDate(targetStart)
                                    : targetEnd,
                            source.isRolloverEnabled(),
                            source.getAlertThresholdPct());
                    Budget saved = persistencePort.save(copy);
                    eventPublisherPort.publish(new BudgetCreated(
                            saved.getId(), saved.getUserId(), saved.getCategoryId(), saved.getAmount()));
                    overwrittenCount++;
                }
            } else {
                // No conflict: create new budget
                Budget copy = Budget.create(
                        command.userId(),
                        source.getCategoryId(),
                        source.getPeriodType(),
                        source.getAmount(),
                        targetStart,
                        source.getPeriodType() != BudgetPeriod.CUSTOM
                                ? source.getPeriodType().calculateEndDate(targetStart)
                                : targetEnd,
                        source.isRolloverEnabled(),
                        source.getAlertThresholdPct());
                Budget saved = persistencePort.save(copy);
                eventPublisherPort.publish(new BudgetCreated(
                        saved.getId(), saved.getUserId(), saved.getCategoryId(), saved.getAmount()));
                copiedCount++;
            }
        }

        return new CopyBudgetsResult(copiedCount, skippedCount, conflictCount, overwrittenCount);
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
