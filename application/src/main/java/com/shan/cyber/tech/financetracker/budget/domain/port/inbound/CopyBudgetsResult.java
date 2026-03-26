package com.shan.cyber.tech.financetracker.budget.domain.port.inbound;

public record CopyBudgetsResult(
    int copiedCount,
    int skippedCount,
    int conflictCount,
    int overwrittenCount
) {}