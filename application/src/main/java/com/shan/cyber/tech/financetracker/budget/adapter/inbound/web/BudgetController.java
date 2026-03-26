package com.shan.cyber.tech.financetracker.budget.adapter.inbound.web;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Budgets", description = "Create and manage spending budgets by category and period. Includes budget plan (spreadsheet) view.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeactivateBudgetUseCase deactivateBudgetUseCase;
    private final GetBudgetsQuery getBudgetsQuery;
    private final GetBudgetPlanQuery getBudgetPlanQuery;
    private final UpsertBudgetByCategoryUseCase upsertBudgetByCategoryUseCase;
    private final BudgetRequestMapper budgetRequestMapper;

    public BudgetController(CreateBudgetUseCase createBudgetUseCase,
                             UpdateBudgetUseCase updateBudgetUseCase,
                             DeactivateBudgetUseCase deactivateBudgetUseCase,
                             GetBudgetsQuery getBudgetsQuery,
                             GetBudgetPlanQuery getBudgetPlanQuery,
                             UpsertBudgetByCategoryUseCase upsertBudgetByCategoryUseCase,
                             BudgetRequestMapper budgetRequestMapper) {
        this.createBudgetUseCase = createBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.deactivateBudgetUseCase = deactivateBudgetUseCase;
        this.getBudgetsQuery = getBudgetsQuery;
        this.getBudgetPlanQuery = getBudgetPlanQuery;
        this.upsertBudgetByCategoryUseCase = upsertBudgetByCategoryUseCase;
        this.budgetRequestMapper = budgetRequestMapper;
    }

    @Operation(
        summary = "List active budgets",
        description = "Returns a paginated list of all active budgets for the authenticated user. Default page size is 50."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of budgets",
            content = @Content(schema = @Schema(implementation = PageResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
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

    @Operation(
        summary = "Get a budget by ID",
        description = "Returns a single budget including current spend, remaining amount, and alert status. Returns 404 if the budget does not exist or belongs to a different user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget found",
            content = @Content(schema = @Schema(implementation = BudgetResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Budget not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/{id}")
    public BudgetResponseDto getById(
            @Parameter(description = "Budget ID", required = true, example = "10")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        BudgetView view = getBudgetsQuery.getById(new BudgetId(id), userId);
        return toResponseDto(view);
    }

    @Operation(
        summary = "Create a budget",
        description = "Creates a new budget for a category and period. Only one active budget per category/period combination is allowed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Budget created — Location header contains the resource URI",
            content = @Content(schema = @Schema(implementation = BudgetResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — duplicate budget for category and period",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping
    public ResponseEntity<BudgetResponseDto> create(@Valid @RequestBody CreateBudgetRequestDto dto) {
        UserId userId = currentUserId();
        BudgetId budgetId = createBudgetUseCase.createBudget(new CreateBudgetCommand(
                userId,
                new CategoryId(dto.categoryId()),
                budgetRequestMapper.toBudgetPeriod(dto.periodType()),
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

    @Operation(
        summary = "Update a budget",
        description = "Updates the budgeted amount, end date, rollover setting, and alert threshold percentage of an existing budget."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget updated",
            content = @Content(schema = @Schema(implementation = BudgetResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Budget not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PutMapping("/{id}")
    public BudgetResponseDto update(
            @Parameter(description = "Budget ID", required = true, example = "10")
            @PathVariable Long id,
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

    @Operation(
        summary = "Deactivate (soft-delete) a budget",
        description = "Marks the budget as inactive. Historical spend data is retained."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Budget deactivated — no content returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Budget not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Budget ID", required = true, example = "10")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        deactivateBudgetUseCase.deactivateBudget(new BudgetId(id), userId);
    }

    @Operation(
        summary = "Upsert a budget by category",
        description = "Creates a new budget for the given category/period, or updates the amount and end date if one already exists. Idempotent for repeated calls with the same category."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget created or updated",
            content = @Content(schema = @Schema(implementation = BudgetResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping("/upsert-by-category")
    public ResponseEntity<BudgetResponseDto> upsertByCategory(
            @Valid @RequestBody UpsertBudgetByCategoryRequestDto dto) {
        UserId userId = currentUserId();
        BudgetId id = upsertBudgetByCategoryUseCase.upsertBudget(new UpsertBudgetByCategoryCommand(
                userId,
                new CategoryId(dto.categoryId()),
                budgetRequestMapper.toBudgetPeriod(dto.periodType()),
                Money.of(dto.amount(), dto.currency()),
                dto.startDate(),
                dto.endDate()));
        BudgetView view = getBudgetsQuery.getById(id, userId);
        return ResponseEntity.ok(toResponseDto(view));
    }

    @Operation(
        summary = "Get the budget plan (spreadsheet view)",
        description = "Returns a structured budget plan for a date range — income rows and expense groups with budgeted vs actual amounts, variances, and projected monthly/yearly totals."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Budget plan for the specified date range",
            content = @Content(schema = @Schema(implementation = BudgetPlanResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid startDate/endDate query parameters",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/plan")
    public ResponseEntity<BudgetPlanResponseDto> getBudgetPlan(
            @Parameter(description = "Plan start date (yyyy-MM-dd)", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Plan end date (yyyy-MM-dd)", required = true, example = "2025-12-31")
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
