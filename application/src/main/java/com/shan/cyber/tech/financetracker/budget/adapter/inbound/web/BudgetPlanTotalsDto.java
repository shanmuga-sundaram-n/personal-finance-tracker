package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

public record BudgetPlanTotalsDto(
    String totalBudgeted,
    String totalActual,
    String totalVariance,
    double totalPercentUsed,
    String totalMonthly,
    String totalYearly
) {}
