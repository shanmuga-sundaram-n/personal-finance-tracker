package com.shan.cyber.tech.financetracker.account.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class DuplicateAccountNameException extends DomainException {
    public DuplicateAccountNameException(String name) {
        super("DUPLICATE_ACCOUNT_NAME", "An active account named '" + name + "' already exists");
    }
}
