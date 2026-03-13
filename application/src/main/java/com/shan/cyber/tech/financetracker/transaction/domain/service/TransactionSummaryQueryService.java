package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CategorySpending;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetTransactionSummaryQuery;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionSummaryQueryService implements GetTransactionSummaryQuery {

    private final TransactionPersistencePort persistencePort;

    public TransactionSummaryQueryService(TransactionPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public BigDecimal sumByType(UserId userId, String transactionType, LocalDate from, LocalDate to) {
        return persistencePort.sumByType(userId, transactionType, from, to);
    }

    @Override
    public List<CategorySpending> sumByCategory(UserId userId, LocalDate from, LocalDate to, int limit) {
        return persistencePort.sumByCategory(userId, from, to, limit);
    }

    @Override
    public List<TransactionView> recentTransactions(UserId userId, int limit) {
        return persistencePort.findRecent(userId, limit).content();
    }
}
