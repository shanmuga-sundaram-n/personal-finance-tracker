package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.DeleteTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransferResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final CreateTransferUseCase createTransferUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase,
                               DeleteTransactionUseCase deleteTransactionUseCase) {
        this.createTransferUseCase = createTransferUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
    }

    @PostMapping
    public ResponseEntity<TransferResultResponseDto> createTransfer(
            @Valid @RequestBody CreateTransferRequestDto dto) {
        UserId userId = currentUserId();
        TransferResult result = createTransferUseCase.createTransfer(new CreateTransferCommand(
                userId,
                new AccountId(dto.fromAccountId()),
                new AccountId(dto.toAccountId()),
                new CategoryId(dto.categoryId()),
                Money.of(dto.amount(), dto.currency()),
                dto.transactionDate(),
                dto.description()));

        TransferResultResponseDto response = new TransferResultResponseDto(
                result.outboundId().value(), result.inboundId().value());
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + result.outboundId().value()))
                .body(response);
    }

    /**
     * Deletes both legs of a transfer identified by the outbound transaction id.
     * The paired inbound transaction is automatically removed.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransfer(@PathVariable Long id) {
        UserId userId = currentUserId();
        deleteTransactionUseCase.deleteTransaction(new TransactionId(id), userId);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }
}
