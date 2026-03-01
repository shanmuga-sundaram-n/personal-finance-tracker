package com.shan.cyber.tech.financetracker.shared.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}
