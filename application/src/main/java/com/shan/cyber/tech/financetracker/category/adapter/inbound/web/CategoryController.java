package com.shan.cyber.tech.financetracker.category.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CategoryView;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.DeactivateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Categories", description = "Manage transaction categories (income, expense, transfer). Includes system and user-defined categories.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeactivateCategoryUseCase deactivateCategoryUseCase;
    private final GetCategoriesQuery getCategoriesQuery;

    public CategoryController(CreateCategoryUseCase createCategoryUseCase,
                               UpdateCategoryUseCase updateCategoryUseCase,
                               DeactivateCategoryUseCase deactivateCategoryUseCase,
                               GetCategoriesQuery getCategoriesQuery) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deactivateCategoryUseCase = deactivateCategoryUseCase;
        this.getCategoriesQuery = getCategoriesQuery;
    }

    @Operation(
        summary = "List categories",
        description = "Returns all active categories visible to the authenticated user (system defaults plus user-defined). Optionally filter by type: INCOME, EXPENSE, or TRANSFER."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of categories (may be empty)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponseDto.class)))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping
    public List<CategoryResponseDto> list(
            @Parameter(description = "Filter by category type code: INCOME, EXPENSE, or TRANSFER. Omit for all types.", example = "EXPENSE")
            @RequestParam(required = false) String type) {
        UserId userId = currentUserId();
        List<CategoryView> views = (type != null && !type.isBlank())
                ? getCategoriesQuery.getByType(userId, type)
                : getCategoriesQuery.getByOwner(userId);
        return views.stream().map(this::toResponseDto).toList();
    }

    @Operation(
        summary = "Create a custom category",
        description = "Creates a user-defined category. Optionally nest it under an existing parent category. System categories cannot be modified."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created — Location header contains the resource URI",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Parent category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — unknown category type code",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping
    public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CreateCategoryRequestDto dto) {
        UserId userId = currentUserId();
        CategoryId categoryId = createCategoryUseCase.createCategory(new CreateCategoryCommand(
                userId, dto.categoryTypeCode(),
                dto.parentCategoryId() != null ? new CategoryId(dto.parentCategoryId()) : null,
                dto.name(), dto.icon(), dto.color()));

        CategoryView view = getCategoriesQuery.getById(categoryId, userId);
        CategoryResponseDto response = toResponseDto(view);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + categoryId.value()))
                .body(response);
    }

    @Operation(
        summary = "Get a category by ID",
        description = "Returns a single category. Returns 404 if it does not exist or belongs to a different user (unless it is a system category)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @GetMapping("/{id}")
    public CategoryResponseDto getById(
            @Parameter(description = "Category ID", required = true, example = "5")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        CategoryView view = getCategoriesQuery.getById(new CategoryId(id), userId);
        return toResponseDto(view);
    }

    @Operation(
        summary = "Update a category",
        description = "Updates the name, icon, and color of a user-defined category. System categories cannot be modified."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated",
            content = @Content(schema = @Schema(implementation = CategoryResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — attempt to modify a system category",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PutMapping("/{id}")
    public CategoryResponseDto update(
            @Parameter(description = "Category ID", required = true, example = "5")
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequestDto dto) {
        UserId userId = currentUserId();
        CategoryView view = updateCategoryUseCase.updateCategory(new UpdateCategoryCommand(
                new CategoryId(id), userId, dto.name(), dto.icon(), dto.color()));
        return toResponseDto(view);
    }

    @Operation(
        summary = "Deactivate (soft-delete) a category",
        description = "Marks the category as inactive. System categories cannot be deactivated. Transactions referencing the category are not affected."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deactivated — no content returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — attempt to deactivate a system category",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @Parameter(description = "Category ID", required = true, example = "5")
            @PathVariable Long id) {
        UserId userId = currentUserId();
        deactivateCategoryUseCase.deactivateCategory(new CategoryId(id), userId);
    }

    private UserId currentUserId() {
        return new UserId(SecurityContextHolder.getCurrentUserId());
    }

    private CategoryResponseDto toResponseDto(CategoryView view) {
        return new CategoryResponseDto(
                view.id(), view.name(), view.categoryTypeCode(), view.categoryTypeName(),
                view.parentCategoryId(), view.parentCategoryName(),
                view.icon(), view.color(), view.isSystem(), view.isActive(),
                view.createdAt());
    }
}
