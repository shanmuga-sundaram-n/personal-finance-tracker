package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.account.domain.port.inbound.AccountView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountCommand;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.CreateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.DeactivateAccountUseCase;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.GetAccountsQuery;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.NetWorthView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.UpdateAccountCommand;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.UpdateAccountUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Accounts", description = "Create and manage financial accounts (checking, savings, credit cards, loans, etc.)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final DeactivateAccountUseCase deactivateAccountUseCase;
    private final GetAccountsQuery getAccountsQuery;

    public AccountController(CreateAccountUseCase createAccountUseCase,
                              UpdateAccountUseCase updateAccountUseCase,
                              DeactivateAccountUseCase deactivateAccountUseCase,
                              GetAccountsQuery getAccountsQuery) {
        this.createAccountUseCase = createAccountUseCase;
        this.updateAccountUseCase = updateAccountUseCase;
        this.deactivateAccountUseCase = deactivateAccountUseCase;
        this.getAccountsQuery = getAccountsQuery;
    }

    @Operation(
        summary = "List all active accounts",
        description = "Returns all active accounts owned by the authenticated user, ordered by creation date."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of accounts (may be empty)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountResponseDto.class)))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping
    public List<AccountResponseDto> list() {
        UserId userId = currentUserId();
        return getAccountsQuery.getAccountsByOwner(userId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Operation(
        summary = "Create a new account",
        description = "Creates a financial account for the authenticated user. The initial balance sets the starting balance; subsequent transactions adjust the running balance."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created — Location header contains the resource URI",
            content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — unknown account type code",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping
    public ResponseEntity<AccountResponseDto> create(@Valid @RequestBody CreateAccountRequestDto dto) {
        UserId userId = currentUserId();
        AccountId accountId = createAccountUseCase.createAccount(new CreateAccountCommand(
                userId, dto.name(), dto.accountTypeCode(),
                Money.of(dto.initialBalance(), dto.currency()),
                dto.institutionName(), dto.accountNumberLast4()));

        AccountView view = getAccountsQuery.getAccountById(accountId, userId);
        AccountResponseDto response = toResponseDto(view);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + accountId.value()))
                .body(response);
    }

    @Operation(
        summary = "Get an account by ID",
        description = "Returns a single account. Returns 404 if the account does not exist or belongs to a different user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/{id}")
    public AccountResponseDto getById(
            @Parameter(description = "Account ID", required = true, example = "1")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        AccountView view = getAccountsQuery.getAccountById(new AccountId(id), userId);
        return toResponseDto(view);
    }

    @Operation(
        summary = "Update an account",
        description = "Updates the display name and institution name of an account. Account type and currency cannot be changed after creation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account updated",
            content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PutMapping("/{id}")
    public AccountResponseDto update(
            @Parameter(description = "Account ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequestDto dto) {
        UserId userId = currentUserId();
        AccountView view = updateAccountUseCase.updateAccount(new UpdateAccountCommand(
                new AccountId(id), userId, dto.name(), dto.institutionName()));
        return toResponseDto(view);
    }

    @Operation(
        summary = "Deactivate (soft-delete) an account",
        description = "Marks the account as inactive. The account is not physically deleted. All associated transactions remain intact."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account deactivated — no content returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @Parameter(description = "Account ID", required = true, example = "1")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        deactivateAccountUseCase.deactivateAccount(new AccountId(id), userId);
    }

    @Operation(
        summary = "Get net worth summary",
        description = "Returns aggregated total assets, total liabilities, and net worth across all active accounts that are included in net worth calculations."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Net worth summary",
            content = @Content(schema = @Schema(implementation = NetWorthResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/net-worth")
    public NetWorthResponseDto netWorth() {
        UserId userId = currentUserId();
        NetWorthView view = getAccountsQuery.getNetWorth(userId);
        return new NetWorthResponseDto(
                view.totalAssets().amount().toPlainString(),
                view.totalLiabilities().amount().toPlainString(),
                view.netWorth().amount().toPlainString(),
                view.netWorth().currency());
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }

    private AccountResponseDto toResponseDto(AccountView view) {
        return new AccountResponseDto(
                view.id(), view.name(), view.accountTypeCode(), view.accountTypeName(),
                view.currentBalance().amount().toPlainString(),
                view.initialBalance().amount().toPlainString(),
                view.currency(), view.institutionName(), view.accountNumberLast4(),
                view.isActive(), view.includeInNetWorth(), view.isLiability(),
                view.createdAt());
    }
}
