package com.shan.cyber.tech.financetracker.shared.domain.exception;

public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String resourceType, String field, String value) {
        super("CONFLICT", resourceType + " with " + field + " '" + value + "' already exists");
    }
}
