package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

public interface CreateTransferUseCase {
    TransferResult createTransfer(CreateTransferCommand command);
}
