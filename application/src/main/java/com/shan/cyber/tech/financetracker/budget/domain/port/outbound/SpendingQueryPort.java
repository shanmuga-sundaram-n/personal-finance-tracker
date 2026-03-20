package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SpendingQueryPort {

    BigDecimal getSpentAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);

    BigDecimal getIncomeAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);

    Map<CategoryId, BigDecimal> getSpentAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to);

    Map<CategoryId, BigDecimal> getIncomeAmountBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to);
}
