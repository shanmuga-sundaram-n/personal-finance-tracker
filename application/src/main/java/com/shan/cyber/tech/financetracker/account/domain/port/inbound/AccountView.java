package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;

import java.time.OffsetDateTime;

public record AccountView(
        Long id,
        String name,
        String accountTypeCode,
        String accountTypeName,
        Money currentBalance,
        Money initialBalance,
        String currency,
        String institutionName,
        String accountNumberLast4,
        boolean isActive,
        boolean includeInNetWorth,
        boolean isLiability,
        OffsetDateTime createdAt) {
}
