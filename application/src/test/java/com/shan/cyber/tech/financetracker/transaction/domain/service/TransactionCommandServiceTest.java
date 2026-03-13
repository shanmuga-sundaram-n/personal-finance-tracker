package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransactionNotFoundException;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransferSameAccountException;
import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransactionCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.BalanceUpdatePort;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionEventPublisherPort;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceTest {

    @Mock private TransactionPersistencePort persistencePort;
    @Mock private TransactionEventPublisherPort eventPublisherPort;
    @Mock private BalanceUpdatePort balanceUpdatePort;

    private TransactionCommandService service;

    private static final UserId USER = new UserId(1L);
    private static final AccountId ACCOUNT = new AccountId(10L);
    private static final AccountId ACCOUNT_B = new AccountId(20L);
    private static final CategoryId CATEGORY = new CategoryId(5L);
    private static final Money AMOUNT = Money.of("100.00", "USD");
    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new TransactionCommandService(persistencePort, eventPublisherPort, balanceUpdatePort);
    }

    @Test
    void createTransaction_income_creditsBalance() {
        CreateTransactionCommand command = new CreateTransactionCommand(
                USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.INCOME, TODAY, "Salary", null, null);

        Transaction saved = Transaction.create(USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.INCOME, TODAY, "Salary", null, null);
        saved.setId(new TransactionId(1L));

        when(persistencePort.save(any(Transaction.class))).thenReturn(saved);

        TransactionId result = service.createTransaction(command);

        assertNotNull(result);
        assertEquals(1L, result.value());
        verify(balanceUpdatePort).credit(ACCOUNT, USER, AMOUNT);
        verify(balanceUpdatePort, never()).debit(any(), any(), any());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void createTransaction_expense_debitsBalance() {
        CreateTransactionCommand command = new CreateTransactionCommand(
                USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.EXPENSE, TODAY, "Groceries", null, null);

        Transaction saved = Transaction.create(USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.EXPENSE, TODAY, "Groceries", null, null);
        saved.setId(new TransactionId(2L));

        when(persistencePort.save(any(Transaction.class))).thenReturn(saved);

        TransactionId result = service.createTransaction(command);

        assertNotNull(result);
        verify(balanceUpdatePort).debit(ACCOUNT, USER, AMOUNT);
        verify(balanceUpdatePort, never()).credit(any(), any(), any());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void createTransfer_sameAccount_throwsTransferSameAccountException() {
        CreateTransferCommand command = new CreateTransferCommand(
                USER, ACCOUNT, ACCOUNT, CATEGORY, AMOUNT, TODAY, "Self transfer");

        assertThrows(TransferSameAccountException.class, () -> service.createTransfer(command));
        verify(persistencePort, never()).saveAll(any());
    }

    @Test
    void createTransfer_differentAccounts_createsTwoLegsAndUpdatesBalances() {
        CreateTransferCommand command = new CreateTransferCommand(
                USER, ACCOUNT, ACCOUNT_B, CATEGORY, AMOUNT, TODAY, "Transfer");

        Transaction outbound = Transaction.create(USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.TRANSFER_OUT, TODAY, "Transfer", null, null);
        outbound.setId(new TransactionId(10L));
        Transaction inbound = Transaction.create(USER, ACCOUNT_B, CATEGORY, AMOUNT, TransactionType.TRANSFER_IN, TODAY, "Transfer", null, null);
        inbound.setId(new TransactionId(11L));

        when(persistencePort.saveAll(any())).thenReturn(List.of(outbound, inbound))
                .thenReturn(List.of(outbound, inbound));

        var result = service.createTransfer(command);

        assertNotNull(result);
        assertEquals(10L, result.outboundId().value());
        assertEquals(11L, result.inboundId().value());
        verify(balanceUpdatePort).debit(ACCOUNT, USER, AMOUNT);
        verify(balanceUpdatePort).credit(ACCOUNT_B, USER, AMOUNT);
        verify(eventPublisherPort, times(2)).publish(any());
    }

    @Test
    void deleteTransaction_expense_reversesDebitAndPublishesEvent() {
        TransactionId txId = new TransactionId(1L);
        Transaction tx = Transaction.create(USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.EXPENSE, TODAY, "Food", null, null);
        tx.setId(txId);

        when(persistencePort.findById(txId, USER)).thenReturn(Optional.of(tx));

        service.deleteTransaction(txId, USER);

        verify(persistencePort).delete(txId);
        verify(balanceUpdatePort).reverseDebit(ACCOUNT, USER, AMOUNT);
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void deleteTransaction_income_reversesCreditAndPublishesEvent() {
        TransactionId txId = new TransactionId(1L);
        Transaction tx = Transaction.create(USER, ACCOUNT, CATEGORY, AMOUNT, TransactionType.INCOME, TODAY, "Salary", null, null);
        tx.setId(txId);

        when(persistencePort.findById(txId, USER)).thenReturn(Optional.of(tx));

        service.deleteTransaction(txId, USER);

        verify(persistencePort).delete(txId);
        verify(balanceUpdatePort).reverseCredit(ACCOUNT, USER, AMOUNT);
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void deleteTransaction_notFound_throwsTransactionNotFoundException() {
        TransactionId txId = new TransactionId(999L);
        when(persistencePort.findById(txId, USER)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> service.deleteTransaction(txId, USER));
        verify(persistencePort, never()).delete(any());
    }
}
