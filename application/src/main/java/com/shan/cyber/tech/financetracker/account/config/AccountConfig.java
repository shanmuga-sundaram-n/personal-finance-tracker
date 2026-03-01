package com.shan.cyber.tech.financetracker.account.config;

import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountEventPublisherPort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountPersistencePort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountTypePersistencePort;
import com.shan.cyber.tech.financetracker.account.domain.service.AccountCommandService;
import com.shan.cyber.tech.financetracker.account.domain.service.AccountQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountConfig {

    @Bean
    public AccountCommandService accountCommandService(
            AccountPersistencePort accountPersistencePort,
            AccountTypePersistencePort accountTypePersistencePort,
            AccountEventPublisherPort accountEventPublisherPort) {
        return new AccountCommandService(accountPersistencePort, accountTypePersistencePort, accountEventPublisherPort);
    }

    @Bean
    public AccountQueryService accountQueryService(AccountPersistencePort accountPersistencePort) {
        return new AccountQueryService(accountPersistencePort);
    }
}
