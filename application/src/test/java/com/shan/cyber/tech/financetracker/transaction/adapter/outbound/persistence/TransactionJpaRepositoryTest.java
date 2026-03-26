package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.config.AbstractPersistenceTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence adapter tests for TransactionJpaRepository complex JPQL queries.
 *
 * Uses native SQL for prerequisite data (users, accounts, categories) to avoid
 * protected-constructor access restrictions on cross-package JPA entities.
 * TransactionJpaEntity is in the same package and is instantiated directly.
 *
 * Reference data seeded by Liquibase:
 *   account_types: id=1 CHECKING, id=2 SAVINGS
 *   category_types: id=1 INCOME, id=2 EXPENSE
 *   system categories: seeded with nextval IDs (not predictable — we insert our own)
 */
@Tag("adapter")
class TransactionJpaRepositoryTest extends AbstractPersistenceTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TransactionJpaRepository repository;

    // Fixed reference dates — deterministic regardless of when tests run
    private static final LocalDate DATE_JAN_01 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DATE_JAN_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DATE_FEB_01 = LocalDate.of(2026, 2, 1);
    private static final LocalDate DATE_MAR_01 = LocalDate.of(2026, 3, 1);
    private static final LocalDate DATE_MAR_31 = LocalDate.of(2026, 3, 31);

    private static final BigDecimal MIN_AMOUNT = BigDecimal.ZERO;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.9999");

    // Entities created per test — IDs populated after native-SQL insert
    private Long ownerUserId;
    private Long otherUserId;
    private Long ownerAccountId;
    private Long otherAccountId;
    private Long expenseCategoryId;
    private Long incomeCategoryId;

    @BeforeEach
    void setUp() {
        // Insert users via native SQL to avoid protected-constructor restriction
        ownerUserId = insertUser("txrepo_owner", "txrepo_owner@test.example.com");
        otherUserId = insertUser("txrepo_other", "txrepo_other@test.example.com");

        // Accounts — account_type_id=1 (CHECKING) from Liquibase seed
        ownerAccountId = insertAccount(ownerUserId, 1, "Owner Checking", "USD");
        otherAccountId = insertAccount(otherUserId, 1, "Other Checking", "USD");

        // Categories — category_type_id=2 (EXPENSE), id=1 (INCOME) from Liquibase seed
        expenseCategoryId = insertCategory(ownerUserId, 2, "Test Groceries Repo");
        incomeCategoryId  = insertCategory(ownerUserId, 1, "Test Salary Repo");

        em.flush();
    }

    // -------------------------------------------------------------------------
    // findByFilter — userId filtering
    // -------------------------------------------------------------------------

    @Test
    void should_returnOnlyOwnerTransactions_when_filterByUserId() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("50.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(otherUserId, otherAccountId, expenseCategoryId,
                new BigDecimal("75.00"), "EXPENSE", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, null, null,
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAmount().compareTo(new BigDecimal("50.00"))).isZero();
    }

    // -------------------------------------------------------------------------
    // findByFilter — accountId filtering
    // -------------------------------------------------------------------------

    @Test
    void should_returnOnlyMatchingAccountTransactions_when_filterByAccountId() {
        // Arrange — two transactions for owner, different accounts
        Long secondAccountId = insertAccount(ownerUserId, 1, "Owner Savings Repo", "USD");
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("120.50"), "EXPENSE", DATE_JAN_15);
        insertTransaction(ownerUserId, secondAccountId, expenseCategoryId,
                new BigDecimal("200.00"), "EXPENSE", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act — filter by the first account only
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, ownerAccountId, null, null,
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAccountId()).isEqualTo(ownerAccountId);
    }

    // -------------------------------------------------------------------------
    // findByFilter — categoryId filtering
    // -------------------------------------------------------------------------

    @Test
    void should_returnOnlyMatchingCategoryTransactions_when_filterByCategoryId() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("45.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("3000.00"), "INCOME", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act — filter to expense category only
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, expenseCategoryId, null,
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCategoryId()).isEqualTo(expenseCategoryId);
    }

    // -------------------------------------------------------------------------
    // findByFilter — transactionType filtering
    // -------------------------------------------------------------------------

    @Test
    void should_returnOnlyExpenseTransactions_when_filterByTransactionTypeExpense() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("88.00"), "EXPENSE", DATE_FEB_01);
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("5000.00"), "INCOME", DATE_FEB_01);
        em.flush();
        em.clear();

        // Act
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, null, "EXPENSE",
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTransactionType()).isEqualTo("EXPENSE");
    }

    @Test
    void should_returnOnlyIncomeTransactions_when_filterByTransactionTypeIncome() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("30.00"), "EXPENSE", DATE_FEB_01);
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("4500.00"), "INCOME", DATE_FEB_01);
        em.flush();
        em.clear();

        // Act
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, null, "INCOME",
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTransactionType()).isEqualTo("INCOME");
    }

    // -------------------------------------------------------------------------
    // findByFilter — date range filtering
    // -------------------------------------------------------------------------

    @Test
    void should_returnTransactionsWithinDateRange_when_filterByFromDateAndToDate() {
        // Arrange — Jan transaction (in range), Mar transaction (outside narrowed range)
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("100.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("200.00"), "EXPENSE", DATE_MAR_01);
        em.flush();
        em.clear();

        // Act — query only January
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, null, null,
                DATE_JAN_01, LocalDate.of(2026, 1, 31), MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTransactionDate()).isEqualTo(DATE_JAN_15);
    }

    @Test
    void should_includeBoundaryDates_when_filterByExactFromAndToDate() {
        // Arrange — transactions on the exact boundary dates
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("50.00"), "EXPENSE", DATE_JAN_01);
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("60.00"), "EXPENSE", DATE_MAR_31);
        em.flush();
        em.clear();

        // Act
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, null, null,
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert — both boundary dates are inclusive
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // findByFilter — combined filters
    // -------------------------------------------------------------------------

    @Test
    void should_returnSingleTransaction_when_filterByCategoryAndTypeAndDateRange() {
        // Arrange — three transactions, only one matches all criteria
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("300.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("150.00"), "EXPENSE", DATE_MAR_01);   // outside Jan range
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("3500.00"), "INCOME", DATE_JAN_15);   // wrong type
        em.flush();
        em.clear();

        // Act — Jan only, EXPENSE type, expense category
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, expenseCategoryId, "EXPENSE",
                DATE_JAN_01, LocalDate.of(2026, 1, 31), MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAmount().compareTo(new BigDecimal("300.00"))).isZero();
    }

    // -------------------------------------------------------------------------
    // findByFilter — pagination
    // -------------------------------------------------------------------------

    @Test
    void should_returnCorrectPage_when_paginationApplied() {
        // Arrange — 5 transactions on different dates (ordered DESC by date)
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("10.00"), "EXPENSE", LocalDate.of(2026, 1, 5));
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("20.00"), "EXPENSE", LocalDate.of(2026, 1, 10));
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("30.00"), "EXPENSE", LocalDate.of(2026, 1, 15));
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("40.00"), "EXPENSE", LocalDate.of(2026, 1, 20));
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("50.00"), "EXPENSE", LocalDate.of(2026, 1, 25));
        em.flush();
        em.clear();

        // Act — page size 2, first page → should be the two most recent
        Page<TransactionViewProjection> page0 = repository.findByFilter(
                ownerUserId, null, null, null,
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 2));

        // Assert
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page0.getContent()).hasSize(2);
        // Ordered DESC by date — first item should be Jan 25 ($50)
        assertThat(page0.getContent().get(0).getAmount().compareTo(new BigDecimal("50.00"))).isZero();
        assertThat(page0.getContent().get(1).getAmount().compareTo(new BigDecimal("40.00"))).isZero();
    }

    @Test
    void should_returnSecondPage_when_pageIndexIsOne() {
        // Arrange — 3 transactions on distinct dates
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("11.00"), "EXPENSE", LocalDate.of(2026, 1, 1));
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("22.00"), "EXPENSE", LocalDate.of(2026, 1, 2));
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("33.00"), "EXPENSE", LocalDate.of(2026, 1, 3));
        em.flush();
        em.clear();

        // Act — page size 2, second page → one item
        Page<TransactionViewProjection> page1 = repository.findByFilter(
                ownerUserId, null, null, null,
                DATE_JAN_01, DATE_MAR_31, MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(1, 2));

        // Assert
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(1);
        // Ordered DESC — oldest (Jan 1, $11) lands on page index 1
        assertThat(page1.getContent().get(0).getAmount().compareTo(new BigDecimal("11.00"))).isZero();
    }

    @Test
    void should_returnEmptyPage_when_noTransactionsMatchFilter() {
        // Arrange — transaction exists but outside the query date range
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("99.00"), "EXPENSE", DATE_MAR_01);
        em.flush();
        em.clear();

        // Act — query a date range with no matches
        Page<TransactionViewProjection> result = repository.findByFilter(
                ownerUserId, null, null, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), MIN_AMOUNT, MAX_AMOUNT,
                PageRequest.of(0, 10));

        // Assert
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // sumExpenseBatch — multiple categories
    // -------------------------------------------------------------------------

    @Test
    void should_returnExpenseTotalsPerCategory_when_multipleExpenseTransactionsExist() {
        // Arrange — two expense categories
        Long catFoodId = insertCategory(ownerUserId, 2, "Test Food Repo");
        em.flush();

        // expenseCategoryId: $45 + $55 = $100
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("45.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("55.00"), "EXPENSE", DATE_FEB_01);

        // catFoodId: $200
        insertTransaction(ownerUserId, ownerAccountId, catFoodId,
                new BigDecimal("200.00"), "EXPENSE", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumExpenseBatch(
                ownerUserId,
                List.of(expenseCategoryId, catFoodId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert
        assertThat(results).hasSize(2);

        CategoryAmountProjection groceriesTotal = results.stream()
                .filter(r -> r.getCategoryId().equals(expenseCategoryId))
                .findFirst()
                .orElseThrow();
        assertThat(groceriesTotal.getTotalAmount().compareTo(new BigDecimal("100.00"))).isZero();

        CategoryAmountProjection foodTotal = results.stream()
                .filter(r -> r.getCategoryId().equals(catFoodId))
                .findFirst()
                .orElseThrow();
        assertThat(foodTotal.getTotalAmount().compareTo(new BigDecimal("200.00"))).isZero();
    }

    @Test
    void should_excludeIncomeTransactions_when_sumExpenseBatch() {
        // Arrange — income transaction in the same date range must NOT be counted
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("150.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("3000.00"), "INCOME", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumExpenseBatch(
                ownerUserId,
                List.of(expenseCategoryId, incomeCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert — only the expense category appears in results
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategoryId()).isEqualTo(expenseCategoryId);
        assertThat(results.get(0).getTotalAmount().compareTo(new BigDecimal("150.00"))).isZero();
    }

    @Test
    void should_returnEmptyList_when_sumExpenseBatchHasNoMatchingTransactions() {
        // Arrange — no transactions inserted

        // Act
        List<CategoryAmountProjection> results = repository.sumExpenseBatch(
                ownerUserId,
                List.of(expenseCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void should_respectDateBoundaries_when_sumExpenseBatch() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("250.00"), "EXPENSE", DATE_JAN_15);             // inside range
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("100.00"), "EXPENSE", LocalDate.of(2025, 12, 31)); // outside range
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumExpenseBatch(
                ownerUserId,
                List.of(expenseCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert — only the in-range transaction is summed
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalAmount().compareTo(new BigDecimal("250.00"))).isZero();
    }

    @Test
    void should_isolateByUser_when_sumExpenseBatch() {
        // Arrange — same category, both users
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("500.00"), "EXPENSE", DATE_JAN_15);
        insertTransaction(otherUserId, otherAccountId, expenseCategoryId,
                new BigDecimal("999.00"), "EXPENSE", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act — query only for ownerUserId
        List<CategoryAmountProjection> results = repository.sumExpenseBatch(
                ownerUserId,
                List.of(expenseCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert — only owner's $500, not other user's $999
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalAmount().compareTo(new BigDecimal("500.00"))).isZero();
    }

    // -------------------------------------------------------------------------
    // sumIncomeBatch — multiple categories
    // -------------------------------------------------------------------------

    @Test
    void should_returnIncomeTotalsPerCategory_when_multipleIncomeTransactionsExist() {
        // Arrange — two income categories
        Long catFreelanceId = insertCategory(ownerUserId, 1, "Test Freelance Repo");
        em.flush();

        // incomeCategoryId: $4000 + $500 = $4500
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("4000.00"), "INCOME", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("500.00"), "INCOME", DATE_FEB_01);

        // catFreelanceId: $1200
        insertTransaction(ownerUserId, ownerAccountId, catFreelanceId,
                new BigDecimal("1200.00"), "INCOME", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumIncomeBatch(
                ownerUserId,
                List.of(incomeCategoryId, catFreelanceId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert
        assertThat(results).hasSize(2);

        CategoryAmountProjection salaryTotal = results.stream()
                .filter(r -> r.getCategoryId().equals(incomeCategoryId))
                .findFirst()
                .orElseThrow();
        assertThat(salaryTotal.getTotalAmount().compareTo(new BigDecimal("4500.00"))).isZero();

        CategoryAmountProjection freelanceTotal = results.stream()
                .filter(r -> r.getCategoryId().equals(catFreelanceId))
                .findFirst()
                .orElseThrow();
        assertThat(freelanceTotal.getTotalAmount().compareTo(new BigDecimal("1200.00"))).isZero();
    }

    @Test
    void should_excludeExpenseTransactions_when_sumIncomeBatch() {
        // Arrange — expense transaction must NOT count in income sum
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("2500.00"), "INCOME", DATE_JAN_15);
        insertTransaction(ownerUserId, ownerAccountId, expenseCategoryId,
                new BigDecimal("300.00"), "EXPENSE", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumIncomeBatch(
                ownerUserId,
                List.of(incomeCategoryId, expenseCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert — only income category appears
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategoryId()).isEqualTo(incomeCategoryId);
        assertThat(results.get(0).getTotalAmount().compareTo(new BigDecimal("2500.00"))).isZero();
    }

    @Test
    void should_returnEmptyList_when_sumIncomeBatchHasNoMatchingTransactions() {
        // Arrange — no transactions inserted

        // Act
        List<CategoryAmountProjection> results = repository.sumIncomeBatch(
                ownerUserId,
                List.of(incomeCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void should_respectDateBoundaries_when_sumIncomeBatch() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("6000.00"), "INCOME", DATE_FEB_01);               // inside
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("1000.00"), "INCOME", LocalDate.of(2025, 12, 15)); // outside
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumIncomeBatch(
                ownerUserId,
                List.of(incomeCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalAmount().compareTo(new BigDecimal("6000.00"))).isZero();
    }

    @Test
    void should_isolateByUser_when_sumIncomeBatch() {
        // Arrange
        insertTransaction(ownerUserId, ownerAccountId, incomeCategoryId,
                new BigDecimal("3500.00"), "INCOME", DATE_JAN_15);
        insertTransaction(otherUserId, otherAccountId, incomeCategoryId,
                new BigDecimal("8000.00"), "INCOME", DATE_JAN_15);
        em.flush();
        em.clear();

        // Act
        List<CategoryAmountProjection> results = repository.sumIncomeBatch(
                ownerUserId,
                List.of(incomeCategoryId),
                DATE_JAN_01, DATE_MAR_31);

        // Assert — only owner's income
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalAmount().compareTo(new BigDecimal("3500.00"))).isZero();
    }

    // -------------------------------------------------------------------------
    // Native SQL helpers — bypass protected JPA constructors
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Long insertUser(String username, String email) {
        em.getEntityManager().createNativeQuery(
                "INSERT INTO finance_tracker.users " +
                "(username, password_hash, email, first_name, last_name, is_active, preferred_currency, created_at, updated_at) " +
                "VALUES (?, '$2a$10$hashed', ?, 'Test', 'User', true, 'USD', NOW(), NOW())")
                .setParameter(1, username)
                .setParameter(2, email)
                .executeUpdate();
        return ((Number) em.getEntityManager()
                .createNativeQuery("SELECT id FROM finance_tracker.users WHERE username = ?")
                .setParameter(1, username)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private Long insertAccount(Long userId, int accountTypeId, String name, String currency) {
        em.getEntityManager().createNativeQuery(
                "INSERT INTO finance_tracker.accounts " +
                "(user_id, account_type_id, name, initial_balance, current_balance, currency, " +
                "is_active, include_in_net_worth, version, created_at, updated_at) " +
                "VALUES (?, ?, ?, 0, 0, ?, true, true, 0, NOW(), NOW())")
                .setParameter(1, userId)
                .setParameter(2, accountTypeId)
                .setParameter(3, name)
                .setParameter(4, currency)
                .executeUpdate();
        return ((Number) em.getEntityManager()
                .createNativeQuery("SELECT id FROM finance_tracker.accounts WHERE user_id = ? AND name = ?")
                .setParameter(1, userId)
                .setParameter(2, name)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private Long insertCategory(Long userId, int categoryTypeId, String name) {
        em.getEntityManager().createNativeQuery(
                "INSERT INTO finance_tracker.categories " +
                "(user_id, category_type_id, name, is_system, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, false, true, NOW(), NOW())")
                .setParameter(1, userId)
                .setParameter(2, categoryTypeId)
                .setParameter(3, name)
                .executeUpdate();
        return ((Number) em.getEntityManager()
                .createNativeQuery("SELECT id FROM finance_tracker.categories WHERE user_id = ? AND name = ?")
                .setParameter(1, userId)
                .setParameter(2, name)
                .getSingleResult()).longValue();
    }

    private void insertTransaction(Long userId, Long accountId, Long categoryId,
                                   BigDecimal amount, String type, LocalDate date) {
        em.getEntityManager().createNativeQuery(
                "INSERT INTO finance_tracker.transactions " +
                "(user_id, account_id, category_id, amount, transaction_type, transaction_date, " +
                "is_recurring, is_reconciled, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, false, false, NOW(), NOW())")
                .setParameter(1, userId)
                .setParameter(2, accountId)
                .setParameter(3, categoryId)
                .setParameter(4, amount)
                .setParameter(5, type)
                .setParameter(6, date)
                .executeUpdate();
    }
}
