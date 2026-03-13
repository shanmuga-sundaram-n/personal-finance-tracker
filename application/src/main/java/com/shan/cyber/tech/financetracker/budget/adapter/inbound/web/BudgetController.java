package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.DeactivateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
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
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeactivateBudgetUseCase deactivateBudgetUseCase;
    private final GetBudgetsQuery getBudgetsQuery;

    public BudgetController(CreateBudgetUseCase createBudgetUseCase,
                             UpdateBudgetUseCase updateBudgetUseCase,
                             DeactivateBudgetUseCase deactivateBudgetUseCase,
                             GetBudgetsQuery getBudgetsQuery) {
        this.createBudgetUseCase = createBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.deactivateBudgetUseCase = deactivateBudgetUseCase;
        this.getBudgetsQuery = getBudgetsQuery;
    }

    @GetMapping
    public List<BudgetResponseDto> list() {
        UserId userId = currentUserId();
        return getBudgetsQuery.getActiveByUser(userId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @GetMapping("/{id}")
    public BudgetResponseDto getById(@PathVariable Long id) {
        UserId userId = currentUserId();
        BudgetView view = getBudgetsQuery.getById(new BudgetId(id), userId);
        return toResponseDto(view);
    }

    @PostMapping
    public ResponseEntity<BudgetResponseDto> create(@Valid @RequestBody CreateBudgetRequestDto dto) {
        UserId userId = currentUserId();
        BudgetId budgetId = createBudgetUseCase.createBudget(new CreateBudgetCommand(
                userId,
                new CategoryId(dto.categoryId()),
                BudgetPeriod.valueOf(dto.periodType()),
                Money.of(dto.amount(), dto.currency()),
                dto.startDate(),
                dto.endDate(),
                dto.rolloverEnabled(),
                dto.alertThresholdPct()));

        BudgetView view = getBudgetsQuery.getById(budgetId, userId);
        BudgetResponseDto response = toResponseDto(view);
        return ResponseEntity.created(URI.create("/api/v1/budgets/" + budgetId.value()))
                .body(response);
    }

    @PutMapping("/{id}")
    public BudgetResponseDto update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateBudgetRequestDto dto) {
        UserId userId = currentUserId();
        updateBudgetUseCase.updateBudget(new UpdateBudgetCommand(
                new BudgetId(id),
                userId,
                Money.of(dto.amount(), dto.currency()),
                dto.endDate(),
                dto.rolloverEnabled(),
                dto.alertThresholdPct()));

        BudgetView view = getBudgetsQuery.getById(new BudgetId(id), userId);
        return toResponseDto(view);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        UserId userId = currentUserId();
        deactivateBudgetUseCase.deactivateBudget(new BudgetId(id), userId);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }

    private BudgetResponseDto toResponseDto(BudgetView view) {
        return new BudgetResponseDto(
                view.id(), view.categoryId(), view.categoryName(),
                view.periodType(), view.amount(), view.currency(),
                view.startDate(), view.endDate(),
                view.rolloverEnabled(), view.alertThresholdPct(),
                view.isActive(), view.spentAmount(), view.remainingAmount(),
                view.percentUsed(), view.alertTriggered(), view.createdAt());
    }
}
