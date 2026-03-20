package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import java.util.List;

public record BudgetPlanCategoryGroupDto(
    Long parentCategoryId,
    String parentCategoryName,
    List<BudgetPlanCategoryRowDto> rows,
    String groupMonthlyTotal,
    String groupYearlyTotal,
    String groupActualTotal,
    boolean alertTriggered
) {}
