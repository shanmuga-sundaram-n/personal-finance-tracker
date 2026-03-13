package com.shan.cyber.tech.financetracker.budget.config;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.BudgetPersistencePort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.SpendingQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.service.BudgetQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BudgetConfig {

    @Bean
    public BudgetQueryService budgetQueryService(BudgetPersistencePort persistencePort,
                                                  SpendingQueryPort spendingQueryPort,
                                                  CategoryNameQueryPort categoryNameQueryPort) {
        return new BudgetQueryService(persistencePort, spendingQueryPort, categoryNameQueryPort);
    }
}
