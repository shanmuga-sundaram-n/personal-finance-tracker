package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

public record BudgetPlanCategoryRowDto(
    Long categoryId,
    String categoryName,
    Long budgetId,
    String budgetedAmount,
    String actualAmount,
    String varianceAmount,
    double percentUsed,
    boolean hasBudget,
    String frequency,
    String monthlyAmount,
    String yearlyAmount
) {}
