package com.shan.cyber.tech.financetracker.budget.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, Long> {

    Optional<BudgetJpaEntity> findByIdAndUserId(Long id, Long userId);

    List<BudgetJpaEntity> findByUserIdAndIsActiveTrue(Long userId);

    Optional<BudgetJpaEntity> findByUserIdAndCategoryIdAndPeriodTypeAndIsActiveTrue(
            Long userId, Long categoryId, String periodType);

    @Query("SELECT b FROM BudgetJpaEntity b WHERE b.userId = :userId AND b.isActive = true AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<BudgetJpaEntity> findActiveByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
