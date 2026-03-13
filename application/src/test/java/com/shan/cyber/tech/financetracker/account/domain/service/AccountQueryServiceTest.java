package com.shan.cyber.tech.financetracker.account.domain.service;

import com.shan.cyber.tech.financetracker.account.domain.exception.AccountNotFoundException;
import com.shan.cyber.tech.financetracker.account.domain.model.Account;
import com.shan.cyber.tech.financetracker.account.domain.model.AccountType;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.AccountView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.NetWorthView;
import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    @Mock private AccountPersistencePort accountPersistencePort;

    private AccountQueryService service;

    private static final UserId OWNER = new UserId(1L);
    private static final AccountType SAVINGS = new AccountType((short) 2, "SAVINGS", "Savings Account", false, false);
    private static final AccountType CREDIT = new AccountType((short) 3, "CREDIT_CARD", "Credit Card", true, true);

    @BeforeEach
    void setUp() {
        service = new AccountQueryService(accountPersistencePort);
    }

    @Test
    void getAccountsByOwner_returnsAllActiveAccounts() {
        Account a1 = Account.create(OWNER, SAVINGS, "Savings", Money.of("500.00", "USD"), null, null);
        a1.setId(new AccountId(1L));
        Account a2 = Account.create(OWNER, SAVINGS, "Checking", Money.of("1000.00", "USD"), null, null);
        a2.setId(new AccountId(2L));

        when(accountPersistencePort.findActiveByOwner(OWNER)).thenReturn(List.of(a1, a2));

        List<AccountView> result = service.getAccountsByOwner(OWNER);

        assertEquals(2, result.size());
        assertEquals("Savings", result.get(0).name());
        assertEquals("Checking", result.get(1).name());
    }

    @Test
    void getAccountsByOwner_noAccounts_returnsEmptyList() {
        when(accountPersistencePort.findActiveByOwner(OWNER)).thenReturn(List.of());

        List<AccountView> result = service.getAccountsByOwner(OWNER);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAccountById_found_returnsView() {
        AccountId accountId = new AccountId(1L);
        Account account = Account.create(OWNER, SAVINGS, "My Savings", Money.of("1000.00", "USD"), "Chase", null);
        account.setId(accountId);

        when(accountPersistencePort.findById(accountId, OWNER)).thenReturn(Optional.of(account));

        AccountView view = service.getAccountById(accountId, OWNER);

        assertNotNull(view);
        assertEquals(1L, view.id());
        assertEquals("My Savings", view.name());
        assertEquals("SAVINGS", view.accountTypeCode());
        assertEquals("Chase", view.institutionName());
    }

    @Test
    void getAccountById_notFound_throwsAccountNotFoundException() {
        AccountId accountId = new AccountId(999L);
        when(accountPersistencePort.findById(accountId, OWNER)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getAccountById(accountId, OWNER));
    }

    @Test
    void getNetWorth_calculatesAssetsOnly() {
        Account savings = Account.create(OWNER, SAVINGS, "Savings", Money.of("1000.00", "USD"), null, null);
        savings.setId(new AccountId(1L));
        Account checking = Account.create(OWNER, SAVINGS, "Checking", Money.of("500.00", "USD"), null, null);
        checking.setId(new AccountId(2L));

        when(accountPersistencePort.findActiveByOwner(OWNER)).thenReturn(List.of(savings, checking));

        NetWorthView result = service.getNetWorth(OWNER);

        assertTrue(result.totalAssets().isPositive());
        assertTrue(result.totalLiabilities().isZero());
        assertTrue(result.netWorth().isPositive());
    }

    @Test
    void getNetWorth_withLiability_separatesAssetsAndLiabilities() {
        Account savings = Account.create(OWNER, SAVINGS, "Savings", Money.of("2000.00", "USD"), null, null);
        savings.setId(new AccountId(1L));
        Account credit = Account.create(OWNER, CREDIT, "Credit Card", Money.of("500.00", "USD"), null, null);
        credit.setId(new AccountId(2L));

        when(accountPersistencePort.findActiveByOwner(OWNER)).thenReturn(List.of(savings, credit));

        NetWorthView result = service.getNetWorth(OWNER);

        assertTrue(result.totalAssets().isPositive());
        assertTrue(result.totalLiabilities().isPositive());
    }

    @Test
    void getNetWorth_emptyAccounts_returnsZeros() {
        when(accountPersistencePort.findActiveByOwner(OWNER)).thenReturn(List.of());

        NetWorthView result = service.getNetWorth(OWNER);

        assertTrue(result.totalAssets().isZero());
        assertTrue(result.totalLiabilities().isZero());
        assertTrue(result.netWorth().isZero());
    }
}
