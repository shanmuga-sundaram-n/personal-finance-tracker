package com.shan.cyber.tech.financetracker.budget.domain.model;

import java.time.LocalDate;

public enum BudgetPeriod {

    WEEKLY,
    MONTHLY,
    QUARTERLY,
    ANNUALLY,
    CUSTOM;

    public LocalDate calculateEndDate(LocalDate startDate) {
        return switch (this) {
            case WEEKLY -> startDate.plusWeeks(1).minusDays(1);
            case MONTHLY -> startDate.plusMonths(1).minusDays(1);
            case QUARTERLY -> startDate.plusMonths(3).minusDays(1);
            case ANNUALLY -> startDate.plusYears(1).minusDays(1);
            case CUSTOM -> null;
        };
    }
}
