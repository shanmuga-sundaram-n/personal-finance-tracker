package com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class AccountJpaMapper {

    private final AccountTypeJpaMapper accountTypeMapper;

    public AccountJpaMapper(AccountTypeJpaMapper accountTypeMapper) {
        this.accountTypeMapper = accountTypeMapper;
    }

    public Account toDomain(AccountJpaEntity entity) {
        AccountType accountType = accountTypeMapper.toDomain(entity.getAccountType());
        AuditInfo auditInfo = new AuditInfo(
                entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getCreatedBy(), entity.getUpdatedBy());

        return new Account(
                new AccountId(entity.getId()),
                new UserId(entity.getUserId()),
                accountType,
                entity.getName(),
                Money.of(entity.getCurrentBalance(), entity.getCurrency()),
                Money.of(entity.getInitialBalance(), entity.getCurrency()),
                entity.getCurrency(),
                entity.getInstitutionName(),
                entity.getAccountNumberLast4(),
                entity.isActive(),
                entity.isIncludeInNetWorth(),
                entity.getVersion(),
                auditInfo);
    }

    public AccountJpaEntity toJpaEntity(Account account, AccountTypeJpaEntity accountTypeEntity) {
        AccountJpaEntity entity = new AccountJpaEntity();
        if (account.getId() != null) {
            entity.setId(account.getId().value());
        }
        entity.setUserId(account.getOwnerId().value());
        entity.setAccountType(accountTypeEntity);
        entity.setName(account.getName());
        entity.setInitialBalance(account.getInitialBalance().amount());
        entity.setCurrentBalance(account.getCurrentBalance().amount());
        entity.setCurrency(account.getCurrency());
        entity.setInstitutionName(account.getInstitutionName());
        entity.setAccountNumberLast4(account.getAccountNumberLast4());
        entity.setActive(account.isActive());
        entity.setIncludeInNetWorth(account.isIncludeInNetWorth());
        entity.setVersion(account.getVersion());
        return entity;
    }
}
