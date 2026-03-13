package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

public record AccountBalanceSummary(
        Long id,
        String name,
        String balance,
        String currency,
        boolean isLiability) {
}
