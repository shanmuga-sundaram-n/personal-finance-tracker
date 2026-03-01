package com.shan.cyber.tech.financetracker.shared.domain.model;

public record BudgetId(Long value) {
    public BudgetId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("BudgetId must be positive");
        }
    }
}
