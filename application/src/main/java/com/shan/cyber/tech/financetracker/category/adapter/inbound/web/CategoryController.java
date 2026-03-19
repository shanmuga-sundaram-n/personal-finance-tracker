package com.shan.cyber.tech.financetracker.category.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CategoryView;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.DeactivateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryUseCase;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.dto.PageResponseDto;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public PageResponseDto<CategoryResponseDto> list(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 50) Pageable pageable) {
        UserId userId = currentUserId();
        List<CategoryView> views;
        if (type != null && !type.isBlank()) {
            views = getCategoriesQuery.getByType(userId, type);
        } else {
            views = getCategoriesQuery.getByOwner(userId);
        }
        List<CategoryResponseDto> allDtos = views.stream().map(this::toResponseDto).toList();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int total = allDtos.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<CategoryResponseDto> pageContent = allDtos.subList(fromIndex, toIndex);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponseDto<>(pageContent, page, size, total, totalPages);
    }

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

    @GetMapping("/{id}")
    public CategoryResponseDto getById(@PathVariable Long id) {
        UserId userId = currentUserId();
        CategoryView view = getCategoriesQuery.getById(new CategoryId(id), userId);
        return toResponseDto(view);
    }

    @PutMapping("/{id}")
    public CategoryResponseDto update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateCategoryRequestDto dto) {
        UserId userId = currentUserId();
        CategoryView view = updateCategoryUseCase.updateCategory(new UpdateCategoryCommand(
                new CategoryId(id), userId, dto.name(), dto.icon(), dto.color()));
        return toResponseDto(view);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
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
