package com.shan.cyber.tech.financetracker.shared.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super("NOT_FOUND", resourceType + " with id " + id + " not found");
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super("NOT_FOUND", resourceType + " '" + identifier + "' not found");
    }
}
