package com.shan.cyber.tech.financetracker.transaction.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;

public class TransferSameAccountException extends BusinessRuleException {
    public TransferSameAccountException() {
        super("TRANSFER_SAME_ACCOUNT", "Transfer source and destination accounts must be different");
    }
}
