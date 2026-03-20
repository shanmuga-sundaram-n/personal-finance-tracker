package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;



public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {

    Optional<TransactionJpaEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByAccountId(Long accountId);

    @Query("""
            SELECT t.id AS id,
                   t.accountId AS accountId,
                   a.name AS accountName,
                   t.categoryId AS categoryId,
                   c.name AS categoryName,
                   t.amount AS amount,
                   a.currency AS currency,
                   t.transactionType AS transactionType,
                   t.transactionDate AS transactionDate,
                   t.description AS description,
                   t.merchantName AS merchantName,
                   t.referenceNumber AS referenceNumber,
                   t.transferPairId AS transferPairId,
                   t.isRecurring AS isRecurring,
                   t.isReconciled AS isReconciled,
                   t.createdAt AS createdAt
            FROM TransactionJpaEntity t
            JOIN com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence.AccountJpaEntity a
                ON a.id = t.accountId
            JOIN com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence.CategoryJpaEntity c
                ON c.id = t.categoryId
            WHERE t.userId = :userId
              AND (:accountId IS NULL OR t.accountId = :accountId)
              AND (:categoryId IS NULL OR t.categoryId = :categoryId)
              AND (:transactionType IS NULL OR t.transactionType = :transactionType)
              AND t.transactionDate >= :fromDate
              AND t.transactionDate <= :toDate
              AND t.amount >= :minAmount
              AND t.amount <= :maxAmount
            ORDER BY t.transactionDate DESC, t.id DESC
            """)
    Page<TransactionViewProjection> findByFilter(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("transactionType") String transactionType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);

    @Query("""
            SELECT t.id AS id,
                   t.accountId AS accountId,
                   a.name AS accountName,
                   t.categoryId AS categoryId,
                   c.name AS categoryName,
                   t.amount AS amount,
                   a.currency AS currency,
                   t.transactionType AS transactionType,
                   t.transactionDate AS transactionDate,
                   t.description AS description,
                   t.merchantName AS merchantName,
                   t.referenceNumber AS referenceNumber,
                   t.transferPairId AS transferPairId,
                   t.isRecurring AS isRecurring,
                   t.isReconciled AS isReconciled,
                   t.createdAt AS createdAt
            FROM TransactionJpaEntity t
            JOIN com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence.AccountJpaEntity a
                ON a.id = t.accountId
            JOIN com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence.CategoryJpaEntity c
                ON c.id = t.categoryId
            WHERE t.id = :id AND t.userId = :userId
            """)
    Optional<TransactionViewProjection> findViewByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionJpaEntity t " +
           "WHERE t.userId = :userId AND t.categoryId = :categoryId " +
           "AND t.transactionType = 'INCOME' " +
           "AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumIncomeAmountByCategoryAndDateRange(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM TransactionJpaEntity t
            WHERE t.userId = :userId
              AND t.categoryId = :categoryId
              AND t.transactionType = 'EXPENSE'
              AND t.transactionDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumExpenseAmount(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM TransactionJpaEntity t
            WHERE t.userId = :userId
              AND t.transactionType = :transactionType
              AND t.transactionDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumByType(
            @Param("userId") Long userId,
            @Param("transactionType") String transactionType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT t.categoryId AS categoryId,
                   c.name AS categoryName,
                   SUM(t.amount) AS totalAmount
            FROM TransactionJpaEntity t
            JOIN com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence.CategoryJpaEntity c
                ON c.id = t.categoryId
            WHERE t.userId = :userId
              AND t.transactionType IN ('EXPENSE', 'TRANSFER_OUT')
              AND t.transactionDate BETWEEN :fromDate AND :toDate
            GROUP BY t.categoryId, c.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategorySpendingProjection> sumByCategory(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT t.id AS id,
                   t.accountId AS accountId,
                   a.name AS accountName,
                   t.categoryId AS categoryId,
                   c.name AS categoryName,
                   t.amount AS amount,
                   a.currency AS currency,
                   t.transactionType AS transactionType,
                   t.transactionDate AS transactionDate,
                   t.description AS description,
                   t.merchantName AS merchantName,
                   t.referenceNumber AS referenceNumber,
                   t.transferPairId AS transferPairId,
                   t.isRecurring AS isRecurring,
                   t.isReconciled AS isReconciled,
                   t.createdAt AS createdAt
            FROM TransactionJpaEntity t
            JOIN com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence.AccountJpaEntity a
                ON a.id = t.accountId
            JOIN com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence.CategoryJpaEntity c
                ON c.id = t.categoryId
            WHERE t.userId = :userId
            ORDER BY t.transactionDate DESC, t.id DESC
            """)
    Page<TransactionViewProjection> findRecentByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT t.categoryId AS categoryId, COALESCE(SUM(t.amount), 0) AS totalAmount
            FROM TransactionJpaEntity t
            WHERE t.userId = :userId
              AND t.categoryId IN :categoryIds
              AND t.transactionType = 'EXPENSE'
              AND t.transactionDate BETWEEN :from AND :to
            GROUP BY t.categoryId
            """)
    List<CategoryAmountProjection> sumExpenseBatch(
            @Param("userId") Long userId,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT t.categoryId AS categoryId, COALESCE(SUM(t.amount), 0) AS totalAmount
            FROM TransactionJpaEntity t
            WHERE t.userId = :userId
              AND t.categoryId IN :categoryIds
              AND t.transactionType = 'INCOME'
              AND t.transactionDate BETWEEN :from AND :to
            GROUP BY t.categoryId
            """)
    List<CategoryAmountProjection> sumIncomeBatch(
            @Param("userId") Long userId,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
