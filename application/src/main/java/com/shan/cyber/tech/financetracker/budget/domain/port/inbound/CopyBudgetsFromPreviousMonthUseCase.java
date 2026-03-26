package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

public interface CopyBudgetsFromPreviousMonthUseCase {

    CopyBudgetsResult copyFromPreviousMonth(CopyBudgetsFromPreviousMonthCommand command);
}
