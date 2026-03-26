package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.dto.PageResponseDto;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Transactions", description = "Record and manage financial transactions — income, expenses, and transfers")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateTransferUseCase createTransferUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final ReconcileTransactionUseCase reconcileTransactionUseCase;
    private final GetTransactionsQuery getTransactionsQuery;
    private final TransactionRequestMapper transactionRequestMapper;
    private final int defaultSize;
    private final int maxSize;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                  CreateTransferUseCase createTransferUseCase,
                                  UpdateTransactionUseCase updateTransactionUseCase,
                                  DeleteTransactionUseCase deleteTransactionUseCase,
                                  ReconcileTransactionUseCase reconcileTransactionUseCase,
                                  GetTransactionsQuery getTransactionsQuery,
                                  TransactionRequestMapper transactionRequestMapper,
                                  @Value("${app.pagination.default-size:30}") int defaultSize,
                                  @Value("${app.pagination.max-size:100}") int maxSize) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.createTransferUseCase = createTransferUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
        this.reconcileTransactionUseCase = reconcileTransactionUseCase;
        this.getTransactionsQuery = getTransactionsQuery;
        this.transactionRequestMapper = transactionRequestMapper;
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
    }

    @Operation(
        summary = "List transactions with optional filters",
        description = "Returns a paginated list of transactions for the authenticated user. Supports filtering by account, category, type, date range, and amount range. Page size is capped at the server maximum (default 100)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated transaction list",
            content = @Content(schema = @Schema(implementation = PageResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping
    public PageResponseDto<TransactionResponseDto> list(
            @Parameter(description = "Filter by account ID", example = "1")
            @RequestParam(required = false) Long accountId,
            @Parameter(description = "Filter by category ID", example = "3")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by transaction type: INCOME, EXPENSE, or TRANSFER", example = "EXPENSE")
            @RequestParam(required = false) String type,
            @Parameter(description = "Start date filter (inclusive), format yyyy-MM-dd", example = "2025-01-01")
            @RequestParam(required = false) LocalDate from,
            @Parameter(description = "End date filter (inclusive), format yyyy-MM-dd", example = "2025-12-31")
            @RequestParam(required = false) LocalDate to,
            @Parameter(description = "Minimum absolute amount filter", example = "10.00")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum absolute amount filter", example = "500.00")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (capped at server maximum)", example = "30")
            @RequestParam(required = false) Integer size) {
        UserId userId = currentUserId();
        int effectiveSize = Math.min(size != null ? size : defaultSize, maxSize);

        TransactionFilter filter = new TransactionFilter(
                userId,
                accountId != null ? new AccountId(accountId) : null,
                categoryId != null ? new CategoryId(categoryId) : null,
                transactionRequestMapper.toTransactionTypeOrNull(type),
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

    @Operation(
        summary = "Create a transaction",
        description = "Records a new INCOME or EXPENSE transaction. For transfers between accounts, use POST /api/v1/transfers instead."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transaction created — Location header contains the resource URI",
            content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Account or category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — e.g. type mismatch with category",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping
    public ResponseEntity<TransactionResponseDto> create(@Valid @RequestBody CreateTransactionRequestDto dto) {
        UserId userId = currentUserId();
        TransactionId transactionId = createTransactionUseCase.createTransaction(new CreateTransactionCommand(
                userId,
                new AccountId(dto.accountId()),
                new CategoryId(dto.categoryId()),
                Money.of(dto.amount(), dto.currency()),
                transactionRequestMapper.toTransactionType(dto.type()),
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
    @Operation(
        summary = "Create a transfer (deprecated)",
        description = "Deprecated. Use POST /api/v1/transfers instead. Creates a paired debit/credit transfer between two accounts. Kept for backward compatibility.",
        deprecated = true
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer created — outbound and inbound transaction IDs returned",
            content = @Content(schema = @Schema(implementation = TransferResultResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Source or destination account not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
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

    @Operation(
        summary = "Get a transaction by ID",
        description = "Returns a single transaction. Returns 404 if the transaction does not exist or belongs to a different user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found",
            content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Transaction not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/{id}")
    public TransactionResponseDto getById(
            @Parameter(description = "Transaction ID", required = true, example = "42")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        TransactionView view = getTransactionsQuery.getById(new TransactionId(id), userId);
        return toResponseDto(view);
    }

    @Operation(
        summary = "Update a transaction",
        description = "Updates editable fields of a transaction: category, amount, date, description, merchant name, and reference number. The account and transaction type are immutable."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction updated",
            content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Transaction or category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PutMapping("/{id}")
    public TransactionResponseDto update(
            @Parameter(description = "Transaction ID", required = true, example = "42")
            @PathVariable Long id,
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

    @Operation(
        summary = "Delete a transaction",
        description = "Permanently deletes a single transaction and reverses its effect on the account balance. For transfers, use DELETE /api/v1/transfers/{id} to remove both legs atomically."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transaction deleted — no content returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Transaction not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Transaction ID", required = true, example = "42")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        deleteTransactionUseCase.deleteTransaction(new TransactionId(id), userId);
    }

    @Operation(
        summary = "Mark a transaction as reconciled or unreconciled",
        description = "Toggles the reconciled flag on a transaction. Reconciled transactions are locked against accidental edits."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reconciliation status updated",
            content = @Content(schema = @Schema(implementation = TransactionResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Transaction not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PatchMapping("/{id}/reconcile")
    public TransactionResponseDto reconcile(
            @Parameter(description = "Transaction ID", required = true, example = "42")
            @PathVariable Long id,
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
