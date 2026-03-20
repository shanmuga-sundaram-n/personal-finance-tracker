package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import java.math.BigDecimal;

public record BudgetPlanCategoryRow(
    Long categoryId,
    String categoryName,
    Long budgetId,
    BigDecimal budgetedAmount,
    BigDecimal actualAmount,
    BigDecimal varianceAmount,
    double percentUsed,
    boolean hasBudget,
    String frequency,
    BigDecimal monthlyAmount,
    BigDecimal yearlyAmount
) {}
