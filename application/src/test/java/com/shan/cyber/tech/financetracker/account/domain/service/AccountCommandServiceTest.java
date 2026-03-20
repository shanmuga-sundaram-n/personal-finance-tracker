package com.shan.cyber.tech.financetracker.account.domain.service;

import com.shan.cyber.tech.financetracker.account.domain.exception.AccountNotFoundException;
import com.shan.cyber.tech.financetracker.account.domain.exception.DuplicateAccountNameException;
import com.shan.cyber.tech.financetracker.account.domain.exception.InsufficientFundsException;
import com.shan.cyber.tech.financetracker.account.domain.exception.MaxAccountsExceededException;
import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountCommand;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountEventPublisherPort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountPersistencePort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountTransactionCountPort;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountTypePersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCommandServiceTest {

    @Mock private AccountPersistencePort accountPersistencePort;
    @Mock private AccountTypePersistencePort accountTypePersistencePort;
    @Mock private AccountEventPublisherPort eventPublisherPort;
    @Mock private AccountTransactionCountPort transactionCountPort;

    private AccountCommandService service;

    private static final UserId OWNER = new UserId(1L);
    private static final AccountType SAVINGS = new AccountType((short) 2, "SAVINGS", "Savings Account", false, false);
    private static final AccountType CHECKING = new AccountType((short) 1, "CHECKING", "Checking Account", true, false);

    @BeforeEach
    void setUp() {
        service = new AccountCommandService(accountPersistencePort, accountTypePersistencePort, eventPublisherPort, transactionCountPort);
    }

    @Test
    void createAccount_success() {
        CreateAccountCommand command = new CreateAccountCommand(
                OWNER, "My Savings", "SAVINGS", Money.of("1000.00", "USD"), null, null);
        when(accountPersistencePort.countActiveByOwner(OWNER)).thenReturn(0L);
        when(accountPersistencePort.findByOwnerAndName(OWNER, "My Savings")).thenReturn(Optional.empty());
        when(accountTypePersistencePort.findByCode("SAVINGS")).thenReturn(Optional.of(SAVINGS));
        when(accountPersistencePort.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            a.setId(new AccountId(1L));
            return a;
        });

        AccountId result = service.createAccount(command);

        assertNotNull(result);
        assertEquals(1L, result.value());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void createAccount_maxExceeded_throws() {
        CreateAccountCommand command = new CreateAccountCommand(
                OWNER, "New Account", "SAVINGS", Money.of("0", "USD"), null, null);
        when(accountPersistencePort.countActiveByOwner(OWNER)).thenReturn(20L);

        assertThrows(MaxAccountsExceededException.class, () -> service.createAccount(command));
        verify(accountPersistencePort, never()).save(any());
    }

    @Test
    void createAccount_duplicateName_throws() {
        CreateAccountCommand command = new CreateAccountCommand(
                OWNER, "My Savings", "SAVINGS", Money.of("0", "USD"), null, null);
        Account existing = Account.create(OWNER, SAVINGS, "My Savings", Money.of("0", "USD"), null, null);
        existing.setId(new AccountId(99L));

        when(accountPersistencePort.countActiveByOwner(OWNER)).thenReturn(5L);
        when(accountPersistencePort.findByOwnerAndName(OWNER, "My Savings")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateAccountNameException.class, () -> service.createAccount(command));
    }

    @Test
    void debit_savingsAccount_insufficientFunds_throws() {
        Account account = Account.create(OWNER, SAVINGS, "Savings", Money.of("100.00", "USD"), null, null);
        account.setId(new AccountId(1L));

        assertThrows(InsufficientFundsException.class,
                () -> account.debit(Money.of("200.00", "USD")));
    }

    @Test
    void debit_checkingAccount_allowsNegative() {
        Account account = Account.create(OWNER, CHECKING, "Checking", Money.of("100.00", "USD"), null, null);
        account.setId(new AccountId(1L));

        account.debit(Money.of("200.00", "USD"));

        assertTrue(account.getCurrentBalance().isNegative());
    }

    @Test
    void credit_addsToBalance() {
        Account account = Account.create(OWNER, SAVINGS, "Savings", Money.of("100.00", "USD"), null, null);
        account.setId(new AccountId(1L));

        account.credit(Money.of("50.00", "USD"));

        assertEquals(Money.of("150.0000", "USD"), account.getCurrentBalance());
    }

    @Test
    void deactivateAccount_notFound_throws() {
        when(accountPersistencePort.findById(new AccountId(99L), OWNER)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.deactivateAccount(new AccountId(99L), OWNER));
    }

    @Test
    void canDebit_savingsWithInsufficientFunds_returnsFalse() {
        Account account = Account.create(OWNER, SAVINGS, "Savings", Money.of("50.00", "USD"), null, null);
        account.setId(new AccountId(1L));

        assertFalse(account.canDebit(Money.of("100.00", "USD")));
    }
}
