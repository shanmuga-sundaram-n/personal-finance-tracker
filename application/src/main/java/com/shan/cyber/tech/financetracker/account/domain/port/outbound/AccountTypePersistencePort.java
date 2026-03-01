package com.shan.cyber.tech.financetracker.account.domain.port.outbound;

import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;

import java.util.List;
import java.util.Optional;

public interface AccountTypePersistencePort {
    Optional<AccountType> findByCode(String code);
    List<AccountType> findAll();
}
