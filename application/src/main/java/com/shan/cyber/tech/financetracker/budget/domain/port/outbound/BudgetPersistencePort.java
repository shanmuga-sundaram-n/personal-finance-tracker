package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface BudgetPersistencePort {

    Budget save(Budget budget);

    Optional<Budget> findById(BudgetId id, UserId userId);

    List<Budget> findActiveByUser(UserId userId);

    Optional<Budget> findActiveByUserAndCategoryAndPeriod(UserId userId, CategoryId categoryId, BudgetPeriod periodType);
}
