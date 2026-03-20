package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.budget.domain.model.BudgetPeriod;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryGroup;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanCategoryRow;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanTotals;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetPlanView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.BudgetView;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.CreateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.DeactivateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetPlanQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.GetBudgetsQuery;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpdateBudgetUseCase;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpsertBudgetByCategoryCommand;
import com.shan.cyber.tech.financetracker.budget.domain.port.inbound.UpsertBudgetByCategoryUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.dto.PageResponseDto;
import com.shan.cyber.tech.financetracker.shared.domain.model.BudgetId;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeactivateBudgetUseCase deactivateBudgetUseCase;
    private final GetBudgetsQuery getBudgetsQuery;
    private final GetBudgetPlanQuery getBudgetPlanQuery;
    private final UpsertBudgetByCategoryUseCase upsertBudgetByCategoryUseCase;

    public BudgetController(CreateBudgetUseCase createBudgetUseCase,
                             UpdateBudgetUseCase updateBudgetUseCase,
                             DeactivateBudgetUseCase deactivateBudgetUseCase,
                             GetBudgetsQuery getBudgetsQuery,
                             GetBudgetPlanQuery getBudgetPlanQuery,
                             UpsertBudgetByCategoryUseCase upsertBudgetByCategoryUseCase) {
        this.createBudgetUseCase = createBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.deactivateBudgetUseCase = deactivateBudgetUseCase;
        this.getBudgetsQuery = getBudgetsQuery;
        this.getBudgetPlanQuery = getBudgetPlanQuery;
        this.upsertBudgetByCategoryUseCase = upsertBudgetByCategoryUseCase;
    }

    @GetMapping
    public PageResponseDto<BudgetResponseDto> list(@PageableDefault(size = 50) Pageable pageable) {
        UserId userId = currentUserId();
        List<BudgetResponseDto> allDtos = getBudgetsQuery.getActiveByUser(userId).stream()
                .map(this::toResponseDto)
                .toList();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int total = allDtos.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<BudgetResponseDto> pageContent = allDtos.subList(fromIndex, toIndex);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponseDto<>(pageContent, page, size, total, totalPages);
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

    @PostMapping("/upsert-by-category")
    public ResponseEntity<BudgetResponseDto> upsertByCategory(
            @Valid @RequestBody UpsertBudgetByCategoryRequestDto dto) {
        UserId userId = currentUserId();
        BudgetId id = upsertBudgetByCategoryUseCase.upsertBudget(new UpsertBudgetByCategoryCommand(
                userId,
                new CategoryId(dto.categoryId()),
                BudgetPeriod.valueOf(dto.periodType()),
                Money.of(dto.amount(), dto.currency()),
                dto.startDate(),
                dto.endDate()));
        BudgetView view = getBudgetsQuery.getById(id, userId);
        return ResponseEntity.ok(toResponseDto(view));
    }

    @GetMapping("/plan")
    public ResponseEntity<BudgetPlanResponseDto> getBudgetPlan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UserId userId = currentUserId();
        BudgetPlanView view = getBudgetPlanQuery.getBudgetPlan(userId, startDate, endDate);
        return ResponseEntity.ok(mapToPlanDto(view));
    }

    private BudgetPlanResponseDto mapToPlanDto(BudgetPlanView view) {
        return new BudgetPlanResponseDto(
                view.startDate().toString(),
                view.endDate().toString(),
                view.currency(),
                view.incomeRows().stream().map(this::mapRowDto).toList(),
                view.expenseGroups().stream().map(this::mapGroupDto).toList(),
                mapTotalsDto(view.incomeTotals()),
                mapTotalsDto(view.expenseTotals())
        );
    }

    private BudgetPlanCategoryGroupDto mapGroupDto(BudgetPlanCategoryGroup group) {
        return new BudgetPlanCategoryGroupDto(
                group.parentCategoryId(),
                group.parentCategoryName(),
                group.rows().stream().map(this::mapRowDto).toList(),
                group.groupMonthlyTotal().toPlainString(),
                group.groupYearlyTotal().toPlainString(),
                group.groupActualTotal().toPlainString(),
                group.alertTriggered()
        );
    }

    private BudgetPlanCategoryRowDto mapRowDto(BudgetPlanCategoryRow row) {
        return new BudgetPlanCategoryRowDto(
                row.categoryId(),
                row.categoryName(),
                row.budgetId(),
                row.budgetedAmount().toPlainString(),
                row.actualAmount().toPlainString(),
                row.varianceAmount().toPlainString(),
                row.percentUsed(),
                row.hasBudget(),
                row.frequency(),
                row.monthlyAmount().toPlainString(),
                row.yearlyAmount().toPlainString()
        );
    }

    private BudgetPlanTotalsDto mapTotalsDto(BudgetPlanTotals totals) {
        return new BudgetPlanTotalsDto(
                totals.totalBudgeted().toPlainString(),
                totals.totalActual().toPlainString(),
                totals.totalVariance().toPlainString(),
                totals.totalPercentUsed(),
                totals.totalMonthly().toPlainString(),
                totals.totalYearly().toPlainString()
        );
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
                view.percentUsed(), view.alertTriggered(),
                view.rolloverAmountAdded(), view.effectiveBudgetThisPeriod(),
                view.createdAt());
    }
}
