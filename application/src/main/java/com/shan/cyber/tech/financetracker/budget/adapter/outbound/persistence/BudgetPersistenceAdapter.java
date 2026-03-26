package com.shan.cyber.tech.financetracker.budget.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.budget.domain.model.Budget;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class BudgetPersistenceAdapter implements BudgetPersistencePort {

    private final BudgetJpaRepository repository;
    private final BudgetJpaMapper mapper;

    public BudgetPersistenceAdapter(BudgetJpaRepository repository, BudgetJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Budget save(Budget budget) {
        BudgetJpaEntity entity = mapper.toJpaEntity(budget);
        BudgetJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Budget> findById(BudgetId id, UserId userId) {
        return repository.findByIdAndUserId(id.value(), userId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Budget> findActiveByUser(UserId userId) {
        return repository.findByUserIdAndIsActiveTrue(userId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Budget> findActiveByUserAndCategoryAndPeriod(UserId userId, CategoryId categoryId, BudgetPeriod periodType) {
        return repository.findByUserIdAndCategoryIdAndPeriodTypeAndIsActiveTrue(
                userId.value(), categoryId.value(), periodType.name())
                .map(mapper::toDomain);
    }

    @Override
    public List<Budget> findActiveByUserAndDateRange(UserId userId, LocalDate startDate, LocalDate endDate) {
        return repository.findActiveByUserAndDateRange(userId.value(), startDate, endDate).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
