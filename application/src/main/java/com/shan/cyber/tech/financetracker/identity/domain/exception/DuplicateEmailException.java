package com.shan.cyber.tech.financetracker.identity.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class DuplicateEmailException extends DomainException {
    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "Email '" + email + "' is already registered");
    }
}
