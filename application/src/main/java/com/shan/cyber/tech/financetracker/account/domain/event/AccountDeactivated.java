package com.shan.cyber.tech.financetracker.account.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record AccountDeactivated(AccountId accountId, UserId ownerId) implements DomainEvent {
}
