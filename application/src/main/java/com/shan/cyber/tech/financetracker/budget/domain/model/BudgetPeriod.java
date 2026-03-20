package com.shan.cyber.tech.financetracker.budget.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public enum BudgetPeriod {

    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUALLY,
    CUSTOM;

    public BigDecimal toMonthlyMultiplier() {
        return switch (this) {
            case WEEKLY -> new BigDecimal("13").divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
            case BI_WEEKLY -> new BigDecimal("13").divide(new BigDecimal("6"), 10, RoundingMode.HALF_UP);
            case MONTHLY -> BigDecimal.ONE;
            case QUARTERLY -> BigDecimal.ONE.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
            case SEMI_ANNUAL -> BigDecimal.ONE.divide(new BigDecimal("6"), 10, RoundingMode.HALF_UP);
            case ANNUALLY -> BigDecimal.ONE.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
            case CUSTOM -> BigDecimal.ONE;
        };
    }

    public LocalDate calculateEndDate(LocalDate startDate) {
        return switch (this) {
            case WEEKLY -> startDate.plusWeeks(1).minusDays(1);
            case BI_WEEKLY -> startDate.plusWeeks(2).minusDays(1);
            case MONTHLY -> startDate.plusMonths(1).minusDays(1);
            case QUARTERLY -> startDate.plusMonths(3).minusDays(1);
            case SEMI_ANNUAL -> startDate.plusMonths(6).minusDays(1);
            case ANNUALLY -> startDate.plusYears(1).minusDays(1);
            case CUSTOM -> null;
        };
    }
}
