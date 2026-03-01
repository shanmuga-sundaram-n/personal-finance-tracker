package com.shan.cyber.tech.financetracker.shared.domain.model;

public record CategoryId(Long value) {
    public CategoryId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("CategoryId must be positive");
        }
    }
}
