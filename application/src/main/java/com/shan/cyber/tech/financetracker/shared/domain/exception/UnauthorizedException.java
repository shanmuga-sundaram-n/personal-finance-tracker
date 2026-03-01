package com.shan.cyber.tech.financetracker.shared.domain.exception;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
