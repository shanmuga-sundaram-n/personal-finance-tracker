package com.shan.cyber.tech.financetracker.account.domain.service;

import com.shan.cyber.tech.financetracker.account.domain.event.AccountCreated;
import com.shan.cyber.tech.financetracker.account.domain.event.AccountCredited;
import com.shan.cyber.tech.financetracker.account.domain.event.AccountDeactivated;
import com.shan.cyber.tech.financetracker.account.domain.event.AccountDebited;
import com.shan.cyber.tech.financetracker.account.domain.exception.AccountNotFoundException;
import com.shan.cyber.tech.financetracker.account.domain.exception.DuplicateAccountNameException;
import com.shan.cyber.tech.financetracker.account.domain.exception.MaxAccountsExceededException;
import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.AccountView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.ApplyBalanceDeltaUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountCommand;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.DeactivateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.UpdateAccountCommand;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.UpdateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountEventPublisherPort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountPersistencePort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountTypePersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public class AccountCommandService implements CreateAccountUseCase, UpdateAccountUseCase,
        DeactivateAccountUseCase, ApplyBalanceDeltaUseCase {

    private static final int MAX_ACTIVE_ACCOUNTS = 20;

    private final AccountPersistencePort accountPersistencePort;
    private final AccountTypePersistencePort accountTypePersistencePort;
    private final AccountEventPublisherPort eventPublisherPort;

    public AccountCommandService(AccountPersistencePort accountPersistencePort,
                                  AccountTypePersistencePort accountTypePersistencePort,
                                  AccountEventPublisherPort eventPublisherPort) {
        this.accountPersistencePort = accountPersistencePort;
        this.accountTypePersistencePort = accountTypePersistencePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public AccountId createAccount(CreateAccountCommand command) {
        long activeCount = accountPersistencePort.countActiveByOwner(command.ownerId());
        if (activeCount >= MAX_ACTIVE_ACCOUNTS) {
            throw new MaxAccountsExceededException(MAX_ACTIVE_ACCOUNTS);
        }

        accountPersistencePort.findByOwnerAndName(command.ownerId(), command.name())
                .filter(Account::isActive)
                .ifPresent(existing -> { throw new DuplicateAccountNameException(command.name()); });

        AccountType accountType = accountTypePersistencePort.findByCode(command.accountTypeCode())
                .orElseThrow(() -> new BusinessRuleException("INVALID_ACCOUNT_TYPE",
                        "Invalid account type code: " + command.accountTypeCode()));

        Account account = Account.create(command.ownerId(), accountType, command.name(),
                command.initialBalance(), command.institutionName(), command.accountNumberLast4());

        Account saved = accountPersistencePort.save(account);
        eventPublisherPort.publish(new AccountCreated(saved.getId(), saved.getOwnerId(), saved.getName()));

        return saved.getId();
    }

    @Override
    public AccountView updateAccount(UpdateAccountCommand command) {
        Account account = findAccountOrThrow(command.accountId(), command.ownerId());

        if (!account.getName().equalsIgnoreCase(command.name())) {
            accountPersistencePort.findByOwnerAndName(command.ownerId(), command.name())
                    .filter(Account::isActive)
                    .ifPresent(existing -> { throw new DuplicateAccountNameException(command.name()); });
        }

        account.rename(command.name());
        account.setInstitutionName(command.institutionName());

        Account saved = accountPersistencePort.save(account);
        return toView(saved);
    }

    @Override
    public void deactivateAccount(AccountId accountId, UserId requestingUser) {
        Account account = findAccountOrThrow(accountId, requestingUser);
        account.deactivate();
        accountPersistencePort.save(account);
        eventPublisherPort.publish(new AccountDeactivated(accountId, requestingUser));
    }

    @Override
    public void applyDebit(AccountId accountId, UserId userId, Money amount) {
        Account account = findAccountOrThrow(accountId, userId);
        account.debit(amount);
        Account saved = accountPersistencePort.save(account);
        eventPublisherPort.publish(new AccountDebited(accountId, amount, saved.getCurrentBalance()));
    }

    @Override
    public void applyCredit(AccountId accountId, UserId userId, Money amount) {
        Account account = findAccountOrThrow(accountId, userId);
        account.credit(amount);
        Account saved = accountPersistencePort.save(account);
        eventPublisherPort.publish(new AccountCredited(accountId, amount, saved.getCurrentBalance()));
    }

    @Override
    public void reverseDebit(AccountId accountId, UserId userId, Money amount) {
        applyCredit(accountId, userId, amount);
    }

    @Override
    public void reverseCredit(AccountId accountId, UserId userId, Money amount) {
        applyDebit(accountId, userId, amount);
    }

    @Override
    public boolean canDebit(AccountId accountId, UserId userId, Money amount) {
        Account account = findAccountOrThrow(accountId, userId);
        return account.canDebit(amount);
    }

    private Account findAccountOrThrow(AccountId accountId, UserId ownerId) {
        return accountPersistencePort.findById(accountId, ownerId)
                .filter(Account::isActive)
                .orElseThrow(() -> new AccountNotFoundException(accountId.value()));
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
