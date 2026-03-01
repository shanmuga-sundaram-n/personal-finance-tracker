package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountPersistenceAdapter implements AccountPersistencePort {

    private final AccountJpaRepository repository;
    private final AccountTypeJpaRepository accountTypeRepository;
    private final AccountJpaMapper mapper;

    public AccountPersistenceAdapter(AccountJpaRepository repository,
                                      AccountTypeJpaRepository accountTypeRepository,
                                      AccountJpaMapper mapper) {
        this.repository = repository;
        this.accountTypeRepository = accountTypeRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Account> findById(AccountId id, UserId ownerId) {
        return repository.findByIdAndUserId(id.value(), ownerId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Account> findActiveByOwner(UserId ownerId) {
        return repository.findByUserIdAndIsActiveTrue(ownerId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countActiveByOwner(UserId ownerId) {
        return repository.countByUserIdAndIsActiveTrue(ownerId.value());
    }

    @Override
    public Optional<Account> findByOwnerAndName(UserId ownerId, String name) {
        return repository.findByUserIdAndNameIgnoreCase(ownerId.value(), name)
                .map(mapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountTypeJpaEntity accountTypeEntity = accountTypeRepository
                .findByCode(account.getAccountType().getCode())
                .orElseThrow();
        AccountJpaEntity entity = mapper.toJpaEntity(account, accountTypeEntity);
        AccountJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
