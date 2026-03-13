package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

public interface UpdateTransactionUseCase {
    TransactionView updateTransaction(UpdateTransactionCommand command);
}
