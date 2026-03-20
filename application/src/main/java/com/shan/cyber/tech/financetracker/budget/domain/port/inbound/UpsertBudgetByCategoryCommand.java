package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import java.time.LocalDate;

public record UpsertBudgetByCategoryCommand(
    UserId userId,
    CategoryId categoryId,
    BudgetPeriod periodType,
    Money amount,
    LocalDate startDate,
    LocalDate endDate
) {}
