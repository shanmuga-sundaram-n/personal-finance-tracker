package com.shan.cyber.tech.financetracker.shared.domain.exception;

public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super("FORBIDDEN", message);
    }
}
