package com.shan.cyber.tech.financetracker.budget.adapter.outbound.event;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetEventPublisherPort;
import com.shan.cyber.tech.financetracker.shared.adapter.outbound.event.SpringEventPublisherAdapter;
import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

@Component
public class BudgetEventPublisherAdapter implements BudgetEventPublisherPort {

    private final SpringEventPublisherAdapter delegate;

    public BudgetEventPublisherAdapter(SpringEventPublisherAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publish(event);
    }
}
