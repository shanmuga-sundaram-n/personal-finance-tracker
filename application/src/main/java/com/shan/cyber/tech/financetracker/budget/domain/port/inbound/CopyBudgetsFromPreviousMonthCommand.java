package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record CopyBudgetsFromPreviousMonthCommand(
    UserId userId,
    int targetYear,
    int targetMonth,
    boolean overwriteExisting
) {}