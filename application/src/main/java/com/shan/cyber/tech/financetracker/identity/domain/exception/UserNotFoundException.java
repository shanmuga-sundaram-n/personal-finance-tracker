package com.shan.cyber.tech.financetracker.identity.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String message) {
        super("USER_NOT_FOUND", message);
    }
}
