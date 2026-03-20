package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import java.math.BigDecimal;

public record BudgetPlanTotals(
    BigDecimal totalBudgeted,
    BigDecimal totalActual,
    BigDecimal totalVariance,
    double totalPercentUsed,
    BigDecimal totalMonthly,
    BigDecimal totalYearly
) {}
