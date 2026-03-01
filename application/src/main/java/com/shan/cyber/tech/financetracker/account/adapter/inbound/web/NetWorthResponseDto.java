package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

public record NetWorthResponseDto(
        String totalAssets,
        String totalLiabilities,
        String netWorth,
        String currency) {
}
