package com.shan.cyber.tech.financetracker.budget.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, Long> {

    Optional<BudgetJpaEntity> findByIdAndUserId(Long id, Long userId);

    List<BudgetJpaEntity> findByUserIdAndIsActiveTrue(Long userId);

    Optional<BudgetJpaEntity> findByUserIdAndCategoryIdAndPeriodTypeAndIsActiveTrue(
            Long userId, Long categoryId, String periodType);
}
