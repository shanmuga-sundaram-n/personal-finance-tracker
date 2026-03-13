package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface DeleteTransactionUseCase {
    void deleteTransaction(TransactionId transactionId, UserId userId);
}
