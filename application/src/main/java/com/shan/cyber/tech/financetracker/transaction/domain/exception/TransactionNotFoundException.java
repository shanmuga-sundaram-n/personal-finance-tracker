package com.shan.cyber.tech.financetracker.transaction.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.ResourceNotFoundException;

public class TransactionNotFoundException extends ResourceNotFoundException {
    public TransactionNotFoundException(Long transactionId) {
        super("Transaction", transactionId);
    }
}
