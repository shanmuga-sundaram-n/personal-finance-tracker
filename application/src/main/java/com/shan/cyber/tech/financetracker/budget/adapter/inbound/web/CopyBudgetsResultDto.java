package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

public record CopyBudgetsResultDto(
        int copiedCount,
        int skippedCount,
        int conflictCount,
        int overwrittenCount
) {}
