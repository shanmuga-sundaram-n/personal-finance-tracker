package com.shan.cyber.tech.financetracker.budget.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.config.AbstractPersistenceTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence adapter tests for BudgetJpaRepository.findActiveByUserAndDateRange.
 *
 * Query semantics (JPQL):
 *   WHERE b.userId = :userId
 *     AND b.isActive = true
 *     AND b.startDate <= :endDate
 *     AND b.endDate >= :startDate
 *
 * This implements an overlap check: budget overlaps the query window if
 * budget.start <= window.end AND budget.end >= window.start.
 *
 * Uses native SQL for user and category insert helpers to avoid
 * protected-constructor access restriction on cross-package JPA entities.
 * BudgetJpaEntity is in the same package and is instantiated directly.
 */
@Tag("adapter")
class BudgetJpaRepositoryTest extends AbstractPersistenceTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private BudgetJpaRepository repository;

    // Fixed dates — deterministic regardless of when tests run
    private static final LocalDate MARCH_01 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_31 = LocalDate.of(2026, 3, 31);
    private static final LocalDate JAN_01   = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31   = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_01   = LocalDate.of(2026, 2, 1);
    private static final LocalDate DEC_31   = LocalDate.of(2026, 12, 31);

    private Long ownerUserId;
    private Long otherUserId;
    private Long expenseCategoryId;

    @BeforeEach
    void setUp() {
        ownerUserId = insertUser("budgetrepo_owner", "budgetrepo_owner@test.example.com");
        otherUserId = insertUser("budgetrepo_other", "budgetrepo_other@test.example.com");

        // category_type_id=2 is EXPENSE (from Liquibase seed)
        expenseCategoryId = insertCategory(ownerUserId, 2, "Budget Test Category");
        em.flush();
    }

    // -------------------------------------------------------------------------
    // Happy path — active budget overlapping the query period
    // -------------------------------------------------------------------------

    @Test
    void should_returnActiveBudget_when_budgetDateRangeOverlapsQueryPeriod() {
        // Arrange — budget spans Jan–Mar 2026, query is for March 2026
        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("500.00"), JAN_01, MARCH_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUserId()).isEqualTo(ownerUserId);
        assertThat(results.get(0).getAmount().compareTo(new BigDecimal("500.00"))).isZero();
    }

    @Test
    void should_returnActiveBudget_when_budgetStartsExactlyOnQueryEndDate() {
        // Arrange — budget starts on March 31 (query end date boundary)
        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("300.00"), MARCH_31, DEC_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — startDate <= :endDate (Mar 31 <= Mar 31) satisfied → overlaps
        assertThat(results).hasSize(1);
    }

    @Test
    void should_returnActiveBudget_when_budgetEndsExactlyOnQueryStartDate() {
        // Arrange — budget ends on March 1 (query start date boundary)
        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("400.00"), JAN_01, MARCH_01, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — endDate >= :startDate (Mar 1 >= Mar 1) satisfied → overlaps
        assertThat(results).hasSize(1);
    }

    @Test
    void should_returnMultipleActiveBudgets_when_multipleBudgetsOverlapPeriod() {
        // Arrange — two active budgets for different categories, both spanning March
        Long cat2Id = insertCategory(ownerUserId, 2, "Budget Test Category 2");
        em.flush();

        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("200.00"), JAN_01, MARCH_31, true);
        persistBudget(ownerUserId, cat2Id, "MONTHLY",
                new BigDecimal("350.00"), FEB_01, DEC_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert
        assertThat(results).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Excludes inactive budgets
    // -------------------------------------------------------------------------

    @Test
    void should_excludeInactiveBudgets_when_budgetIsActiveIsFalse() {
        // Arrange — inactive budget overlapping the query period
        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("600.00"), JAN_01, DEC_31, false);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — inactive budget must not appear
        assertThat(results).isEmpty();
    }

    @Test
    void should_returnOnlyActiveBudgets_when_mixedActiveAndInactiveBudgetsExist() {
        // Arrange — one active, one inactive, both overlapping March
        Long cat2Id = insertCategory(ownerUserId, 2, "Budget Mixed Category");
        em.flush();

        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("500.00"), JAN_01, DEC_31, true);
        persistBudget(ownerUserId, cat2Id, "MONTHLY",
                new BigDecimal("800.00"), JAN_01, DEC_31, false);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — only the active budget is returned
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAmount().compareTo(new BigDecimal("500.00"))).isZero();
        assertThat(results.get(0).isActive()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Excludes budgets outside the query date range
    // -------------------------------------------------------------------------

    @Test
    void should_excludeBudget_when_budgetEndDateIsBeforeQueryStartDate() {
        // Arrange — budget ended Jan 31, query is for March → no overlap
        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("250.00"), JAN_01, JAN_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — endDate (Jan 31) < startDate (Mar 1) → no overlap
        assertThat(results).isEmpty();
    }

    @Test
    void should_excludeBudget_when_budgetStartDateIsAfterQueryEndDate() {
        // Arrange — budget starts April 1, query is through March 31 → no overlap
        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("700.00"), LocalDate.of(2026, 4, 1), DEC_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — startDate (Apr 1) > endDate (Mar 31) → no overlap
        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // User scoping — other users' budgets must not appear
    // -------------------------------------------------------------------------

    @Test
    void should_excludeOtherUsersBudgets_when_queryByOwnerUserId() {
        // Arrange — budget belonging to otherUserId
        Long otherCatId = insertCategory(otherUserId, 2, "Other User Category");
        em.flush();

        persistBudget(otherUserId, otherCatId, "MONTHLY",
                new BigDecimal("999.00"), JAN_01, DEC_31, true);
        em.flush();
        em.clear();

        // Act — query for ownerUserId only
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — other user's budget must not appear
        assertThat(results).isEmpty();
    }

    @Test
    void should_returnOnlyOwnerBudgets_when_bothUsersHaveOverlappingBudgets() {
        // Arrange
        Long otherCatId = insertCategory(otherUserId, 2, "Other User Rent");
        em.flush();

        persistBudget(ownerUserId, expenseCategoryId, "MONTHLY",
                new BigDecimal("1500.00"), JAN_01, DEC_31, true);
        persistBudget(otherUserId, otherCatId, "MONTHLY",
                new BigDecimal("2000.00"), JAN_01, DEC_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — only owner's $1500 budget
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUserId()).isEqualTo(ownerUserId);
        assertThat(results.get(0).getAmount().compareTo(new BigDecimal("1500.00"))).isZero();
    }

    // -------------------------------------------------------------------------
    // Edge cases — empty results and field value correctness
    // -------------------------------------------------------------------------

    @Test
    void should_returnEmptyList_when_noActiveBudgetsExistForUser() {
        // Arrange — no budgets inserted

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void should_returnCorrectFieldValues_when_activeBudgetFound() {
        // Arrange
        persistBudget(ownerUserId, expenseCategoryId, "QUARTERLY",
                new BigDecimal("1200.00"), JAN_01, MARCH_31, true);
        em.flush();
        em.clear();

        // Act
        List<BudgetJpaEntity> results = repository.findActiveByUserAndDateRange(
                ownerUserId, MARCH_01, MARCH_31);

        // Assert — verify all fields are correctly persisted and retrieved
        assertThat(results).hasSize(1);
        BudgetJpaEntity budget = results.get(0);
        assertThat(budget.getUserId()).isEqualTo(ownerUserId);
        assertThat(budget.getCategoryId()).isEqualTo(expenseCategoryId);
        assertThat(budget.getPeriodType()).isEqualTo("QUARTERLY");
        assertThat(budget.getAmount().compareTo(new BigDecimal("1200.00"))).isZero();
        assertThat(budget.getCurrency()).isEqualTo("USD");
        assertThat(budget.getStartDate()).isEqualTo(JAN_01);
        assertThat(budget.getEndDate()).isEqualTo(MARCH_31);
        assertThat(budget.isActive()).isTrue();
        assertThat(budget.isRolloverEnabled()).isFalse();
        assertThat(budget.getAlertThresholdPct()).isEqualTo((short) 80);
    }

    // -------------------------------------------------------------------------
    // Helpers — native SQL to avoid protected-constructor restrictions
    // -------------------------------------------------------------------------

    private BudgetJpaEntity persistBudget(Long userId, Long categoryId, String periodType,
                                          BigDecimal amount, LocalDate startDate,
                                          LocalDate endDate, boolean active) {
        BudgetJpaEntity budget = new BudgetJpaEntity();
        budget.setUserId(userId);
        budget.setCategoryId(categoryId);
        budget.setPeriodType(periodType);
        budget.setAmount(amount);
        budget.setCurrency("USD");
        budget.setStartDate(startDate);
        budget.setEndDate(endDate);
        budget.setRolloverEnabled(false);
        budget.setAlertThresholdPct((short) 80);
        budget.setActive(active);
        em.persist(budget);
        return budget;
    }

    @SuppressWarnings("unchecked")
    private Long insertUser(String username, String email) {
        em.createNativeQuery(
                "INSERT INTO finance_tracker.users " +
                "(username, password_hash, email, first_name, last_name, is_active, preferred_currency, created_at, updated_at) " +
                "VALUES (?, '$2a$10$hashed', ?, 'Test', 'User', true, 'USD', NOW(), NOW())")
                .setParameter(1, username)
                .setParameter(2, email)
                .executeUpdate();
        return ((Number) em.createNativeQuery(
                "SELECT id FROM finance_tracker.users WHERE username = ?")
                .setParameter(1, username)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private Long insertCategory(Long userId, int categoryTypeId, String name) {
        em.createNativeQuery(
                "INSERT INTO finance_tracker.categories " +
                "(user_id, category_type_id, name, is_system, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, false, true, NOW(), NOW())")
                .setParameter(1, userId)
                .setParameter(2, categoryTypeId)
                .setParameter(3, name)
                .executeUpdate();
        return ((Number) em.createNativeQuery(
                "SELECT id FROM finance_tracker.categories WHERE user_id = ? AND name = ?")
                .setParameter(1, userId)
                .setParameter(2, name)
                .getSingleResult()).longValue();
    }
}
