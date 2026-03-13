package com.shan.cyber.tech.financetracker.reporting.config;

import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.AccountQueryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.BudgetSummaryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.TransactionSummaryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.UserPreferencesPort;
import com.shan.cyber.tech.financetracker.reporting.domain.service.ReportingQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportingConfig {

    @Bean
    public ReportingQueryService reportingQueryService(AccountQueryPort accountQueryPort,
                                                        TransactionSummaryPort transactionSummaryPort,
                                                        BudgetSummaryPort budgetSummaryPort,
                                                        UserPreferencesPort userPreferencesPort) {
        return new ReportingQueryService(accountQueryPort, transactionSummaryPort, budgetSummaryPort, userPreferencesPort);
    }
}
