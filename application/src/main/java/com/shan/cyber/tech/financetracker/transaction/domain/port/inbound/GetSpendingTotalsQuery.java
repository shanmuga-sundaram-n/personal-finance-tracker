package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface GetSpendingTotalsQuery {

    BigDecimal sumExpenses(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);

    BigDecimal sumIncome(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);

    Map<CategoryId, BigDecimal> sumExpenseBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to);

    Map<CategoryId, BigDecimal> sumIncomeBatch(UserId userId, List<CategoryId> categoryIds, LocalDate from, LocalDate to);
}
