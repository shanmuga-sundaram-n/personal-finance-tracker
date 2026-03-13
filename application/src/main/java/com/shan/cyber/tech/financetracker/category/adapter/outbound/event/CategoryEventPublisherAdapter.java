package com.shan.cyber.tech.financetracker.category.adapter.outbound.event;

import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryEventPublisherPort;
import com.shan.cyber.tech.financetracker.shared.adapter.outbound.event.SpringEventPublisherAdapter;
import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

@Component
public class CategoryEventPublisherAdapter implements CategoryEventPublisherPort {

    private final SpringEventPublisherAdapter delegate;

    public CategoryEventPublisherAdapter(SpringEventPublisherAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(DomainEvent event) {
        delegate.publish(event);
    }
}
