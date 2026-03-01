package com.shan.cyber.tech.financetracker.identity.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password");
    }
}
