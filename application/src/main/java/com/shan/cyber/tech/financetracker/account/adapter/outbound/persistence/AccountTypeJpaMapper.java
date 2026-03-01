package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import org.springframework.stereotype.Component;

@Component
public class AccountTypeJpaMapper {

    public AccountType toDomain(AccountTypeJpaEntity entity) {
        return new AccountType(entity.getId(), entity.getCode(), entity.getName(),
                entity.isAllowsNegativeBalance(), entity.isLiability());
    }
}
