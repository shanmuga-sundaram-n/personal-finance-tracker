package com.shan.cyber.tech.financetracker.reporting.domain.port.inbound;

public record RecentTransactionSummary(
        Long id,
        String description,
        String amount,
        String currency,
        String type,
        String categoryName,
        String date) {
}
