package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface GetSpendingTotalsQuery {

    BigDecimal sumExpenses(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);
}
