package com.shan.cyber.tech.financetracker.account.domain.service;

import com.shan.cyber.tech.financetracker.account.domain.exception.AccountNotFoundException;
import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.AccountView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.GetAccountsQuery;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.NetWorthView;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.math.BigDecimal;
import java.util.List;

public class AccountQueryService implements GetAccountsQuery {

    private final AccountPersistencePort accountPersistencePort;

    public AccountQueryService(AccountPersistencePort accountPersistencePort) {
        this.accountPersistencePort = accountPersistencePort;
    }

    @Override
    public List<AccountView> getAccountsByOwner(UserId ownerId) {
        return accountPersistencePort.findActiveByOwner(ownerId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public AccountView getAccountById(AccountId accountId, UserId ownerId) {
        Account account = accountPersistencePort.findById(accountId, ownerId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.value()));
        return toView(account);
    }

    @Override
    public NetWorthView getNetWorth(UserId ownerId) {
        List<Account> accounts = accountPersistencePort.findActiveByOwner(ownerId).stream()
                .filter(Account::isIncludeInNetWorth)
                .toList();

        String currency = "USD";
        Money totalAssets = Money.zero(currency);
        Money totalLiabilities = Money.zero(currency);

        for (Account account : accounts) {
            BigDecimal amount = account.getCurrentBalance().amount();
            if (account.isLiability()) {
                totalLiabilities = totalLiabilities.add(Money.of(amount.abs(), currency));
            } else {
                totalAssets = totalAssets.add(Money.of(amount, currency));
            }
        }

        return new NetWorthView(totalAssets, totalLiabilities, totalAssets.subtract(totalLiabilities));
    }

    private AccountView toView(Account account) {
        return new AccountView(
                account.getId().value(), account.getName(),
                account.getAccountType().getCode(), account.getAccountType().getName(),
                account.getCurrentBalance(), account.getInitialBalance(),
                account.getCurrency(), account.getInstitutionName(),
                account.getAccountNumberLast4(), account.isActive(),
                account.isIncludeInNetWorth(), account.isLiability(),
                account.getAuditInfo() != null ? account.getAuditInfo().createdAt() : null);
    }
}
