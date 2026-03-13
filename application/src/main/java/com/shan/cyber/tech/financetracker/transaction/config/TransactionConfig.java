package com.shan.cyber.tech.financetracker.transaction.config;

import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import com.shan.cyber.tech.financetracker.transaction.domain.service.SpendingTotalsQueryService;
import com.shan.cyber.tech.financetracker.transaction.domain.service.TransactionQueryService;
import com.shan.cyber.tech.financetracker.transaction.domain.service.TransactionSummaryQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfig {

    @Bean
    public TransactionQueryService transactionQueryService(TransactionPersistencePort persistencePort) {
        return new TransactionQueryService(persistencePort);
    }

    @Bean
    public SpendingTotalsQueryService spendingTotalsQueryService(TransactionPersistencePort persistencePort) {
        return new SpendingTotalsQueryService(persistencePort);
    }

    @Bean
    public TransactionSummaryQueryService transactionSummaryQueryService(TransactionPersistencePort persistencePort) {
        return new TransactionSummaryQueryService(persistencePort);
    }
}
