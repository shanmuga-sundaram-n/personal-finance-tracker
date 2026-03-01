package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;

public record NetWorthView(Money totalAssets, Money totalLiabilities, Money netWorth) {
}
