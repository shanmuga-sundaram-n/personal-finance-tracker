package com.shan.cyber.tech.financetracker.shared.domain.model;

public record AccountId(Long value) {
    public AccountId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("AccountId must be positive");
        }
    }
}
