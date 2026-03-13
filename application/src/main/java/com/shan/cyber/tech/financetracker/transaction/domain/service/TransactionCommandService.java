package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.transaction.domain.event.TransactionCreated;
import com.shan.cyber.tech.financetracker.transaction.domain.event.TransactionDeleted;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransactionNotFoundException;
import com.shan.cyber.tech.financetracker.transaction.domain.exception.TransferSameAccountException;
import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransactionCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.DeleteTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransferResult;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.UpdateTransactionCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.UpdateTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.BalanceUpdatePort;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionEventPublisherPort;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;

import java.util.List;

public class TransactionCommandService implements CreateTransactionUseCase, CreateTransferUseCase, DeleteTransactionUseCase, UpdateTransactionUseCase {

    private final TransactionPersistencePort persistencePort;
    private final TransactionEventPublisherPort eventPublisherPort;
    private final BalanceUpdatePort balanceUpdatePort;

    public TransactionCommandService(TransactionPersistencePort persistencePort,
                                      TransactionEventPublisherPort eventPublisherPort,
                                      BalanceUpdatePort balanceUpdatePort) {
        this.persistencePort = persistencePort;
        this.eventPublisherPort = eventPublisherPort;
        this.balanceUpdatePort = balanceUpdatePort;
    }

    @Override
    public TransactionId createTransaction(CreateTransactionCommand command) {
        Transaction transaction = Transaction.create(
                command.userId(), command.accountId(), command.categoryId(),
                command.amount(), command.type(), command.transactionDate(),
                command.description(), command.merchantName(), command.referenceNumber());

        if (transaction.getType().isDebit()) {
            balanceUpdatePort.debit(command.accountId(), command.userId(), command.amount());
        } else {
            balanceUpdatePort.credit(command.accountId(), command.userId(), command.amount());
        }

        Transaction saved = persistencePort.save(transaction);

        eventPublisherPort.publish(new TransactionCreated(
                saved.getId(), saved.getUserId(), saved.getAccountId(),
                saved.getAmount(), saved.getType()));

        return saved.getId();
    }

    @Override
    public TransferResult createTransfer(CreateTransferCommand command) {
        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new TransferSameAccountException();
        }

        // Create both legs
        Transaction outbound = Transaction.create(
                command.userId(), command.fromAccountId(), command.categoryId(),
                command.amount(), TransactionType.TRANSFER_OUT, command.transactionDate(),
                command.description(), null, null);

        Transaction inbound = Transaction.create(
                command.userId(), command.toAccountId(), command.categoryId(),
                command.amount(), TransactionType.TRANSFER_IN, command.transactionDate(),
                command.description(), null, null);

        // First save: persist both without pair links
        List<Transaction> saved = persistencePort.saveAll(List.of(outbound, inbound));
        Transaction savedOutbound = saved.get(0);
        Transaction savedInbound = saved.get(1);

        // Link pair IDs
        savedOutbound.linkTransferPair(savedInbound.getId());
        savedInbound.linkTransferPair(savedOutbound.getId());

        // Second save: persist with pair links
        persistencePort.saveAll(List.of(savedOutbound, savedInbound));

        // Apply balance changes
        balanceUpdatePort.debit(command.fromAccountId(), command.userId(), command.amount());
        balanceUpdatePort.credit(command.toAccountId(), command.userId(), command.amount());

        // Publish events
        eventPublisherPort.publish(new TransactionCreated(
                savedOutbound.getId(), command.userId(), command.fromAccountId(),
                command.amount(), TransactionType.TRANSFER_OUT));
        eventPublisherPort.publish(new TransactionCreated(
                savedInbound.getId(), command.userId(), command.toAccountId(),
                command.amount(), TransactionType.TRANSFER_IN));

        return new TransferResult(savedOutbound.getId(), savedInbound.getId());
    }

    @Override
    public TransactionView updateTransaction(UpdateTransactionCommand command) {
        Transaction transaction = persistencePort.findById(command.transactionId(), command.userId())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionId().value()));

        Money oldAmount = transaction.updateDetails(
                command.categoryId(), command.amount(), command.transactionDate(),
                command.description(), command.merchantName(), command.referenceNumber());

        // Adjust balance if amount changed
        if (!oldAmount.equals(command.amount())) {
            // Reverse old balance effect
            if (transaction.getType().isDebit()) {
                balanceUpdatePort.reverseDebit(transaction.getAccountId(), command.userId(), oldAmount);
                balanceUpdatePort.debit(transaction.getAccountId(), command.userId(), command.amount());
            } else {
                balanceUpdatePort.reverseCredit(transaction.getAccountId(), command.userId(), oldAmount);
                balanceUpdatePort.credit(transaction.getAccountId(), command.userId(), command.amount());
            }
        }

        persistencePort.save(transaction);

        return persistencePort.findViewById(command.transactionId(), command.userId())
                .orElseThrow(() -> new TransactionNotFoundException(command.transactionId().value()));
    }

    @Override
    public void deleteTransaction(TransactionId transactionId, com.shan.cyber.tech.financetracker.shared.domain.model.UserId userId) {
        Transaction transaction = persistencePort.findById(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.value()));

        if (transaction.getType().isTransfer()) {
            deleteTransferPair(transaction, userId);
        } else {
            deleteSingleTransaction(transaction);
        }
    }

    private void deleteTransferPair(Transaction transaction, com.shan.cyber.tech.financetracker.shared.domain.model.UserId userId) {
        TransactionId pairId = transaction.getTransferPairId();
        Transaction pair = null;
        if (pairId != null) {
            pair = persistencePort.findById(pairId, userId).orElse(null);
        }

        // Unlink and delete both
        transaction.unlinkTransferPair();
        persistencePort.save(transaction);
        if (pair != null) {
            pair.unlinkTransferPair();
            persistencePort.save(pair);
            persistencePort.delete(pair.getId());
            reverseBalance(pair);
            eventPublisherPort.publish(new TransactionDeleted(
                    pair.getId(), pair.getUserId(), pair.getAccountId(),
                    pair.getAmount(), pair.getType()));
        }

        persistencePort.delete(transaction.getId());
        reverseBalance(transaction);
        eventPublisherPort.publish(new TransactionDeleted(
                transaction.getId(), transaction.getUserId(), transaction.getAccountId(),
                transaction.getAmount(), transaction.getType()));
    }

    private void deleteSingleTransaction(Transaction transaction) {
        persistencePort.delete(transaction.getId());
        reverseBalance(transaction);
        eventPublisherPort.publish(new TransactionDeleted(
                transaction.getId(), transaction.getUserId(), transaction.getAccountId(),
                transaction.getAmount(), transaction.getType()));
    }

    private void reverseBalance(Transaction transaction) {
        if (transaction.getType().isDebit()) {
            balanceUpdatePort.reverseDebit(transaction.getAccountId(), transaction.getUserId(), transaction.getAmount());
        } else {
            balanceUpdatePort.reverseCredit(transaction.getAccountId(), transaction.getUserId(), transaction.getAmount());
        }
    }
}
