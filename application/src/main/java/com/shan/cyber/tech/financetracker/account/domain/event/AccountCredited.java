package com.shan.cyber.tech.financetracker.account.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;

public record AccountCredited(AccountId accountId, Money amount, Money newBalance) implements DomainEvent {
}
