package com.shan.cyber.tech.financetracker.category.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record CategoryDeactivated(CategoryId categoryId, UserId ownerId) implements DomainEvent {
}
