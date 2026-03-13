package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;

public interface CreateTransactionUseCase {
    TransactionId createTransaction(CreateTransactionCommand command);
}
