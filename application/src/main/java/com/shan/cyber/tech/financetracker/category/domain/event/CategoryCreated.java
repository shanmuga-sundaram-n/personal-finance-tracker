package com.shan.cyber.tech.financetracker.category.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record CategoryCreated(CategoryId categoryId, UserId ownerId, String name) implements DomainEvent {
}
