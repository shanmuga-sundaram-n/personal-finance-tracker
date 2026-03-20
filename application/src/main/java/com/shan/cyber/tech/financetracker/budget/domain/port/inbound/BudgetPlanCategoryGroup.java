package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import java.math.BigDecimal;
import java.util.List;

public record BudgetPlanCategoryGroup(
    Long parentCategoryId,
    String parentCategoryName,
    List<BudgetPlanCategoryRow> rows,
    BigDecimal groupMonthlyTotal,
    BigDecimal groupYearlyTotal,
    BigDecimal groupActualTotal,
    boolean alertTriggered
) {}
