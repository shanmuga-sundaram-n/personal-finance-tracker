package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.dto.PageResponseDto;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransactionCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CreateTransferUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.DeleteTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.GetTransactionsQuery;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.ReconcileTransactionCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.ReconcileTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransferResult;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.UpdateTransactionCommand;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.UpdateTransactionUseCase;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionFilter;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateTransferUseCase createTransferUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final ReconcileTransactionUseCase reconcileTransactionUseCase;
    private final GetTransactionsQuery getTransactionsQuery;
    private final int defaultSize;
    private final int maxSize;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                  CreateTransferUseCase createTransferUseCase,
                                  UpdateTransactionUseCase updateTransactionUseCase,
                                  DeleteTransactionUseCase deleteTransactionUseCase,
                                  ReconcileTransactionUseCase reconcileTransactionUseCase,
                                  GetTransactionsQuery getTransactionsQuery,
                                  @Value("${app.pagination.default-size:30}") int defaultSize,
                                  @Value("${app.pagination.max-size:100}") int maxSize) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.createTransferUseCase = createTransferUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
        this.reconcileTransactionUseCase = reconcileTransactionUseCase;
        this.getTransactionsQuery = getTransactionsQuery;
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
    }

    @GetMapping
    public PageResponseDto<TransactionResponseDto> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        UserId userId = currentUserId();
        int effectiveSize = Math.min(size != null ? size : defaultSize, maxSize);

        TransactionFilter filter = new TransactionFilter(
                userId,
                accountId != null ? new AccountId(accountId) : null,
                categoryId != null ? new CategoryId(categoryId) : null,
                type != null ? TransactionType.valueOf(type) : null,
                from,
                to,
                minAmount,
                maxAmount);

        TransactionPage result = getTransactionsQuery.getTransactions(filter, page, effectiveSize);

        return new PageResponseDto<>(
                result.content().stream().map(this::toResponseDto).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDto> create(@Valid @RequestBody CreateTransactionRequestDto dto) {
        UserId userId = currentUserId();
        TransactionId transactionId = createTransactionUseCase.createTransaction(new CreateTransactionCommand(
                userId,
                new AccountId(dto.accountId()),
                new CategoryId(dto.categoryId()),
                Money.of(dto.amount(), dto.currency()),
                TransactionType.valueOf(dto.type()),
                dto.transactionDate(),
                dto.description(),
                dto.merchantName(),
                dto.referenceNumber()));

        TransactionView view = getTransactionsQuery.getById(transactionId, userId);
        TransactionResponseDto response = toResponseDto(view);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + transactionId.value()))
                .body(response);
    }

    /**
     * @deprecated Use POST /api/v1/transfers instead. Kept for backward compatibility.
     */
    @Deprecated
    @PostMapping("/transfers")
    public ResponseEntity<TransferResultResponseDto> createTransfer(@Valid @RequestBody CreateTransferRequestDto dto) {
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
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + result.outboundId().value()))
                .body(response);
    }

    @GetMapping("/{id}")
    public TransactionResponseDto getById(@PathVariable Long id) {
        UserId userId = currentUserId();
        TransactionView view = getTransactionsQuery.getById(new TransactionId(id), userId);
        return toResponseDto(view);
    }

    @PutMapping("/{id}")
    public TransactionResponseDto update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateTransactionRequestDto dto) {
        UserId userId = currentUserId();
        TransactionView view = updateTransactionUseCase.updateTransaction(new UpdateTransactionCommand(
                new TransactionId(id),
                userId,
                new CategoryId(dto.categoryId()),
                Money.of(dto.amount(), dto.currency()),
                dto.transactionDate(),
                dto.description(),
                dto.merchantName(),
                dto.referenceNumber()));
        return toResponseDto(view);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        UserId userId = currentUserId();
        deleteTransactionUseCase.deleteTransaction(new TransactionId(id), userId);
    }

    @PatchMapping("/{id}/reconcile")
    public TransactionResponseDto reconcile(@PathVariable Long id,
                                             @RequestBody ReconcileTransactionRequestDto dto) {
        UserId userId = currentUserId();
        TransactionView view = reconcileTransactionUseCase.reconcileTransaction(
                new ReconcileTransactionCommand(new TransactionId(id), userId, dto.reconciled()));
        return toResponseDto(view);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }

    private TransactionResponseDto toResponseDto(TransactionView view) {
        return new TransactionResponseDto(
                view.id(), view.accountId(), view.accountName(),
                view.categoryId(), view.categoryName(),
                view.amount(), view.currency(), view.type(),
                view.transactionDate(), view.description(),
                view.merchantName(), view.referenceNumber(),
                view.transferPairId(), view.isRecurring(), view.isReconciled(),
                view.createdAt());
    }
}
