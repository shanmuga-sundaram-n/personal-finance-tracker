package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import java.util.List;

public record BudgetPlanResponseDto(
    String startDate,
    String endDate,
    String currency,
    List<BudgetPlanCategoryRowDto> incomeRows,
    List<BudgetPlanCategoryGroupDto> expenseGroups,
    BudgetPlanTotalsDto incomeTotals,
    BudgetPlanTotalsDto expenseTotals
) {}
