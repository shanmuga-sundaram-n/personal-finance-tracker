package com.shan.cyber.tech.financetracker.account.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException(Long accountId) {
        super("ACCOUNT_NOT_FOUND", "Account with id " + accountId + " not found");
    }
}
