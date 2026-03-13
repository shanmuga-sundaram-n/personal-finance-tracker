package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransactionNotFoundException;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetTransactionsQuery;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionFilter;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPage;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;

public class TransactionQueryService implements GetTransactionsQuery {

    private final TransactionPersistencePort persistencePort;

    public TransactionQueryService(TransactionPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public TransactionPage getTransactions(TransactionFilter filter, int page, int size) {
        return persistencePort.findByFilter(filter, page, size);
    }

    @Override
    public TransactionView getById(TransactionId transactionId, UserId userId) {
        return persistencePort.findViewById(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.value()));
    }
}
