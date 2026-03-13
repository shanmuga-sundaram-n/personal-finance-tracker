package com.shan.cyber.tech.financetracker.transaction.config;

import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
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
import com.shan.cyber.tech.financetracker.transaction.domain.service.TransactionCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionApplicationService implements CreateTransactionUseCase, CreateTransferUseCase, DeleteTransactionUseCase, UpdateTransactionUseCase {

    private final TransactionCommandService commandService;

    public TransactionApplicationService(TransactionPersistencePort persistencePort,
                                          TransactionEventPublisherPort eventPublisherPort,
                                          BalanceUpdatePort balanceUpdatePort) {
        this.commandService = new TransactionCommandService(persistencePort, eventPublisherPort, balanceUpdatePort);
    }

    @Override
    public TransactionId createTransaction(CreateTransactionCommand command) {
        return commandService.createTransaction(command);
    }

    @Override
    public TransferResult createTransfer(CreateTransferCommand command) {
        return commandService.createTransfer(command);
    }

    @Override
    public TransactionView updateTransaction(UpdateTransactionCommand command) {
        return commandService.updateTransaction(command);
    }

    @Override
    public void deleteTransaction(TransactionId transactionId, UserId userId) {
        commandService.deleteTransaction(transactionId, userId);
    }
}
