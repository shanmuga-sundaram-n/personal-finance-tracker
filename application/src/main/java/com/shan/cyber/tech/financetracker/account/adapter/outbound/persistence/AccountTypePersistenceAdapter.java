package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountTypePersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountTypePersistenceAdapter implements AccountTypePersistencePort {

    private final AccountTypeJpaRepository repository;
    private final AccountTypeJpaMapper mapper;

    public AccountTypePersistenceAdapter(AccountTypeJpaRepository repository, AccountTypeJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<AccountType> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<AccountType> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
