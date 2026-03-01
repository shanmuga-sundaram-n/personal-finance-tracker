package com.shan.cyber.tech.financetracker.account.adapter.outbound.event;

import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountEventPublisherPort;
import com.shan.cyber.tech.financetracker.shared.adapter.outbound.event.SpringEventPublisherAdapter;
import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

@Component
public class AccountEventPublisherAdapter implements AccountEventPublisherPort {

    private final SpringEventPublisherAdapter delegate;

    public AccountEventPublisherAdapter(SpringEventPublisherAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publish(event);
    }
}
