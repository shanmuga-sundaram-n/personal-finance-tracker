package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SpendingQueryPort {

    BigDecimal getSpentAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to);
}
