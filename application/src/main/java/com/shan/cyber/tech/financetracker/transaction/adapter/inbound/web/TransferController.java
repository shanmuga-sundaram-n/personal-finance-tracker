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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Transfers", description = "Create and delete paired account-to-account transfers. Each transfer produces two linked transactions (debit + credit).")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
        summary = "Create a transfer between two accounts",
        description = "Atomically creates a debit transaction on the source account and a credit transaction on the destination account. Returns both transaction IDs."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer created — Location header points to the outbound transaction",
            content = @Content(schema = @Schema(implementation = TransferResultResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Source or destination account not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — source and destination accounts are the same",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
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

    @Operation(
        summary = "Delete both legs of a transfer",
        description = "Permanently deletes both the outbound and inbound transactions that form a transfer pair. Supply the outbound transaction ID; the paired inbound transaction is resolved automatically."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Both transfer legs deleted — no content returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Transfer not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransfer(
            @Parameter(description = "Outbound transaction ID of the transfer pair", required = true, example = "7")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        deleteTransactionUseCase.deleteTransaction(new TransactionId(id), userId);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }
}
