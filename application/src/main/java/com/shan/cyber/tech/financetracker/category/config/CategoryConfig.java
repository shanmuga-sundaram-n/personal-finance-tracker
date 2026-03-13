package com.shan.cyber.tech.financetracker.category.config;

import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryEventPublisherPort;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryPersistencePort;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryTypePersistencePort;
import com.shan.cyber.tech.financetracker.category.domain.service.CategoryCommandService;
import com.shan.cyber.tech.financetracker.category.domain.service.CategoryQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoryConfig {

    @Bean
    public CategoryCommandService categoryCommandService(
            CategoryPersistencePort categoryPersistencePort,
            CategoryTypePersistencePort categoryTypePersistencePort,
            CategoryEventPublisherPort categoryEventPublisherPort) {
        return new CategoryCommandService(categoryPersistencePort, categoryTypePersistencePort, categoryEventPublisherPort);
    }

    @Bean
    public CategoryQueryService categoryQueryService(CategoryPersistencePort categoryPersistencePort) {
        return new CategoryQueryService(categoryPersistencePort);
    }
}
