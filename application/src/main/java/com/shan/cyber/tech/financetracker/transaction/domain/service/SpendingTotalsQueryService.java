package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetSpendingTotalsQuery;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SpendingTotalsQueryService implements GetSpendingTotalsQuery {

    private final TransactionPersistencePort persistencePort;

    public SpendingTotalsQueryService(TransactionPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public BigDecimal sumExpenses(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to) {
        return persistencePort.sumExpenseAmount(userId, categoryId, from, to);
    }
}
