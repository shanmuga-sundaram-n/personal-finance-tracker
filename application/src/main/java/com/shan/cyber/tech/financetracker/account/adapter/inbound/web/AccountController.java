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

    @GetMapping
    public List<AccountResponseDto> list() {
        UserId userId = currentUserId();
        return getAccountsQuery.getAccountsByOwner(userId).stream()
                .map(this::toResponseDto)
                .toList();
    }

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

    @GetMapping("/{id}")
    public AccountResponseDto getById(@PathVariable Long id) {
        UserId userId = currentUserId();
        AccountView view = getAccountsQuery.getAccountById(new AccountId(id), userId);
        return toResponseDto(view);
    }

    @PutMapping("/{id}")
    public AccountResponseDto update(@PathVariable Long id,
                                      @Valid @RequestBody UpdateAccountRequestDto dto) {
        UserId userId = currentUserId();
        AccountView view = updateAccountUseCase.updateAccount(new UpdateAccountCommand(
                new AccountId(id), userId, dto.name(), dto.institutionName()));
        return toResponseDto(view);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        UserId userId = currentUserId();
        deactivateAccountUseCase.deactivateAccount(new AccountId(id), userId);
    }

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
