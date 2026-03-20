package com.shan.cyber.tech.financetracker.transaction.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;

import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CategorySpending;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionPersistencePort {

    Transaction save(Transaction transaction);

    List<Transaction> saveAll(List<Transaction> transactions);

    Optional<Transaction> findById(TransactionId id, UserId userId);

    Optional<TransactionView> findViewById(TransactionId id, UserId userId);

    TransactionPage findByFilter(TransactionFilter filter, int page, int size);

    void delete(TransactionId id);

    BigDecimal sumExpenseAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);

    BigDecimal sumIncomeAmountByCategoryAndDateRange(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);

    BigDecimal sumByType(UserId userId, String transactionType, LocalDate from, LocalDate to);

    List<CategorySpending> sumByCategory(UserId userId, LocalDate from, LocalDate to, int limit);

    Map<CategoryId, BigDecimal> sumExpenseAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to);

    Map<CategoryId, BigDecimal> sumIncomeAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to);

    TransactionPage findRecent(UserId userId, int limit);
}
