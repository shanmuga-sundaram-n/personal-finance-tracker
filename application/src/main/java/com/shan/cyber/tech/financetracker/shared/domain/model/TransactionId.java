package com.shan.cyber.tech.financetracker.shared.domain.model;

public record TransactionId(Long value) {
    public TransactionId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("TransactionId must be positive");
        }
    }
}
