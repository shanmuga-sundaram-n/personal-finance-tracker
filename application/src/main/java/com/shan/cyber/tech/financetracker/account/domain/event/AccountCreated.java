package com.shan.cyber.tech.financetracker.account.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record AccountCreated(AccountId accountId, UserId ownerId, String name) implements DomainEvent {
}
