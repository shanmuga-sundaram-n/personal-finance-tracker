package com.shan.cyber.tech.financetracker.identity.adapter.outbound.event;

import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.IdentityEventPublisherPort;
import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.port.outbound.EventPublisherPort;
import org.springframework.stereotype.Component;

@Component
public class IdentityEventPublisherAdapter implements IdentityEventPublisherPort {

    private final EventPublisherPort delegate;

    public IdentityEventPublisherAdapter(EventPublisherPort delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publish(event);
    }
}
