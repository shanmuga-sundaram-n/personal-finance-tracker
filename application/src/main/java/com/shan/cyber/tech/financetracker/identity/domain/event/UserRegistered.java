package com.shan.cyber.tech.financetracker.identity.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record UserRegistered(UserId userId, String email) implements DomainEvent {
}
