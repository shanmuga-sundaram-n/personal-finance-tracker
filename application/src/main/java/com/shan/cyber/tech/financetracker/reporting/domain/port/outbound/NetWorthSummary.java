package com.shan.cyber.tech.financetracker.reporting.domain.port.outbound;

public record NetWorthSummary(String totalAssets, String totalLiabilities, String netWorth, String currency) {
}
