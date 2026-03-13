package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.event;

import com.shan.cyber.tech.financetracker.shared.adapter.outbound.event.SpringEventPublisherAdapter;
import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionEventPublisherPort;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisherAdapter implements TransactionEventPublisherPort {

    private final SpringEventPublisherAdapter delegate;

    public TransactionEventPublisherAdapter(SpringEventPublisherAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publish(event);
    }
}
