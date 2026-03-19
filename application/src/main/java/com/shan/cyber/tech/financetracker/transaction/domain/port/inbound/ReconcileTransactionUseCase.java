package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

public interface ReconcileTransactionUseCase {

    TransactionView reconcileTransaction(ReconcileTransactionCommand command);
}
