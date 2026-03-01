package com.shan.cyber.tech.financetracker.account.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;

public record NetWorth(Money totalAssets, Money totalLiabilities) {
    public Money netWorth() {
        return totalAssets.subtract(totalLiabilities);
    }
}
