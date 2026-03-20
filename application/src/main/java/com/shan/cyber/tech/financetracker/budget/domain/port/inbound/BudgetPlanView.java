package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import java.time.LocalDate;
import java.util.List;

public record BudgetPlanView(
    LocalDate startDate,
    LocalDate endDate,
    String currency,
    List<BudgetPlanCategoryRow> incomeRows,
    List<BudgetPlanCategoryGroup> expenseGroups,
    BudgetPlanTotals incomeTotals,
    BudgetPlanTotals expenseTotals
) {}
