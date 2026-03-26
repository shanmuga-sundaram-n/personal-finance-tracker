package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-1 Regression — Transfer atomicity contract.
 *
 * <p>The transfer flow in TransactionCommandService.createTransfer() must maintain the
 * following invariant: if persistence of the pair-linked transactions fails (second saveAll
 * throws), no account balance mutations are applied. Both debits and credits must remain
 * uncommitted when the persistence layer rejects the transaction.</p>
 *
 * <p>This test uses pure domain service instantiation (no Spring context) to verify the
 * ordering contract: balance mutations happen strictly after all persistence calls succeed.</p>
 */
@ExtendWith(MockitoExtension.class)
class TransferAtomicityTest {

    @Mock private TransactionPersistencePort persistencePort;
    @Mock private TransactionEventPublisherPort eventPublisherPort;
    @Mock private BalanceUpdatePort balanceUpdatePort;

    private TransactionCommandService service;

    private static final UserId USER           = new UserId(42L);
    private static final AccountId FROM_ACCOUNT = new AccountId(100L);
    private static final AccountId TO_ACCOUNT   = new AccountId(200L);
    private static final CategoryId CATEGORY    = new CategoryId(7L);
    private static final Money AMOUNT           = Money.of("450.00", "USD");
    private static final LocalDate TXN_DATE     = LocalDate.of(2026, 3, 26);

    @BeforeEach
    void setUp() {
        service = new TransactionCommandService(persistencePort, eventPublisherPort, balanceUpdatePort);
    }

    @Test
    void should_notMutateEitherAccountBalance_when_secondSaveAllThrows() {
        // Arrange — first saveAll succeeds, second saveAll (pair linking) throws
        Transaction savedOutbound = Transaction.create(
                USER, FROM_ACCOUNT, CATEGORY, AMOUNT, TransactionType.TRANSFER_OUT,
                TXN_DATE, "Rent payment", null, null);
        savedOutbound.setId(new TransactionId(10L));

        Transaction savedInbound = Transaction.create(
                USER, TO_ACCOUNT, CATEGORY, AMOUNT, TransactionType.TRANSFER_IN,
                TXN_DATE, "Rent payment", null, null);
        savedInbound.setId(new TransactionId(11L));

        when(persistencePort.saveAll(any()))
                .thenReturn(List.of(savedOutbound, savedInbound))   // first call: initial save
                .thenThrow(new RuntimeException("DB constraint violation on pair ID update"));

        // Act + Assert — the service must propagate the exception
        assertThatThrownBy(() -> service.createTransfer(
                new CreateTransferCommand(USER, FROM_ACCOUNT, TO_ACCOUNT, CATEGORY, AMOUNT, TXN_DATE, "Rent payment")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB constraint violation");

        // Assert — neither balance mutation was applied because they execute after saveAll
        verify(balanceUpdatePort, never()).debit(any(), any(), any());
        verify(balanceUpdatePort, never()).credit(any(), any(), any());
    }

    @Test
    void should_applyBothBalanceMutations_only_after_persistenceSucceeds() {
        // Arrange — both saveAll calls succeed
        Transaction savedOutbound = Transaction.create(
                USER, FROM_ACCOUNT, CATEGORY, AMOUNT, TransactionType.TRANSFER_OUT,
                TXN_DATE, "Transfer to savings", null, null);
        savedOutbound.setId(new TransactionId(20L));

        Transaction savedInbound = Transaction.create(
                USER, TO_ACCOUNT, CATEGORY, AMOUNT, TransactionType.TRANSFER_IN,
                TXN_DATE, "Transfer to savings", null, null);
        savedInbound.setId(new TransactionId(21L));

        when(persistencePort.saveAll(any()))
                .thenReturn(List.of(savedOutbound, savedInbound))
                .thenReturn(List.of(savedOutbound, savedInbound));

        // Act
        var result = service.createTransfer(
                new CreateTransferCommand(USER, FROM_ACCOUNT, TO_ACCOUNT, CATEGORY, AMOUNT, TXN_DATE, "Transfer to savings"));

        // Assert — both balance mutations applied exactly once after successful persistence
        verify(balanceUpdatePort).debit(FROM_ACCOUNT, USER, AMOUNT);
        verify(balanceUpdatePort).credit(TO_ACCOUNT, USER, AMOUNT);
        verify(eventPublisherPort, org.mockito.Mockito.times(2)).publish(any());
    }
}
