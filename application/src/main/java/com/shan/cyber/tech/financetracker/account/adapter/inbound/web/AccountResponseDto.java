package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

import java.time.OffsetDateTime;

public record AccountResponseDto(
        Long id,
        String name,
        String accountTypeCode,
        String accountTypeName,
        String currentBalance,
        String initialBalance,
        String currency,
        String institutionName,
        String accountNumberLast4,
        boolean isActive,
        boolean includeInNetWorth,
        boolean isLiability,
        OffsetDateTime createdAt) {
}
