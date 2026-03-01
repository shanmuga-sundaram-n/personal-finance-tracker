package com.shan.cyber.tech.financetracker.account.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class MaxAccountsExceededException extends DomainException {
    public MaxAccountsExceededException(int maxAllowed) {
        super("MAX_ACCOUNTS_EXCEEDED", "Maximum of " + maxAllowed + " active accounts allowed");
    }
}
