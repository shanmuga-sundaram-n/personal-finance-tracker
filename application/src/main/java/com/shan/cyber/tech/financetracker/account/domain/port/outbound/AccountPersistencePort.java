package com.shan.cyber.tech.financetracker.account.domain.port.outbound;

import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface AccountPersistencePort {
    Optional<Account> findById(AccountId id, UserId ownerId);
    List<Account> findActiveByOwner(UserId ownerId);
    long countActiveByOwner(UserId ownerId);
    Optional<Account> findByOwnerAndName(UserId ownerId, String name);
    Account save(Account account);
}
