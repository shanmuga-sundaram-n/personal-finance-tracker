package com.shan.cyber.tech.financetracker.identity.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class DuplicateUsernameException extends DomainException {
    public DuplicateUsernameException(String username) {
        super("DUPLICATE_USERNAME", "Username '" + username + "' is already taken");
    }
}
