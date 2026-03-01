package com.shan.cyber.tech.financetracker.account.domain.model;

import com.shan.cyber.tech.financetracker.account.domain.exception.InsufficientFundsException;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.Objects;

public class Account {

    private AccountId id;
    private UserId ownerId;
    private AccountType accountType;
    private String name;
    private Money currentBalance;
    private Money initialBalance;
    private String currency;
    private String institutionName;
    private String accountNumberLast4;
    private boolean isActive;
    private boolean includeInNetWorth;
    private Long version;
    private AuditInfo auditInfo;

    public Account(AccountId id, UserId ownerId, AccountType accountType, String name,
                   Money currentBalance, Money initialBalance, String currency,
                   String institutionName, String accountNumberLast4,
                   boolean isActive, boolean includeInNetWorth, Long version, AuditInfo auditInfo) {
        this.id = id;
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.accountType = Objects.requireNonNull(accountType, "accountType must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.currentBalance = Objects.requireNonNull(currentBalance, "currentBalance must not be null");
        this.initialBalance = Objects.requireNonNull(initialBalance, "initialBalance must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.institutionName = institutionName;
        this.accountNumberLast4 = accountNumberLast4;
        this.isActive = isActive;
        this.includeInNetWorth = includeInNetWorth;
        this.version = version;
        this.auditInfo = auditInfo;
    }

    public static Account create(UserId ownerId, AccountType accountType, String name,
                                  Money initialBalance, String institutionName, String accountNumberLast4) {
        return new Account(null, ownerId, accountType, name, initialBalance, initialBalance,
                initialBalance.currency(), institutionName, accountNumberLast4,
                true, true, 0L, null);
    }

    public void debit(Money amount) {
        Money result = currentBalance.subtract(amount);
        if (!accountType.isAllowsNegativeBalance() && result.isNegative()) {
            throw new InsufficientFundsException(id, result);
        }
        this.currentBalance = result;
    }

    public void credit(Money amount) {
        this.currentBalance = currentBalance.add(amount);
    }

    public boolean canDebit(Money amount) {
        Money result = currentBalance.subtract(amount);
        return accountType.isAllowsNegativeBalance() || !result.isNegative();
    }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName, "name must not be null");
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean isLiability() {
        return accountType.isLiability();
    }

    public AccountId getId() { return id; }
    public UserId getOwnerId() { return ownerId; }
    public AccountType getAccountType() { return accountType; }
    public String getName() { return name; }
    public Money getCurrentBalance() { return currentBalance; }
    public Money getInitialBalance() { return initialBalance; }
    public String getCurrency() { return currency; }
    public String getInstitutionName() { return institutionName; }
    public String getAccountNumberLast4() { return accountNumberLast4; }
    public boolean isActive() { return isActive; }
    public boolean isIncludeInNetWorth() { return includeInNetWorth; }
    public Long getVersion() { return version; }
    public AuditInfo getAuditInfo() { return auditInfo; }

    public void setId(AccountId id) { this.id = id; }
    public void setVersion(Long version) { this.version = version; }
    public void setAuditInfo(AuditInfo auditInfo) { this.auditInfo = auditInfo; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
}
