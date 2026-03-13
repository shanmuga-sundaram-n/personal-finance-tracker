package com.shan.cyber.tech.financetracker.category.domain.service;

import com.shan.cyber.tech.financetracker.category.domain.exception.CategoryNotFoundException;
import com.shan.cyber.tech.financetracker.category.domain.model.Category;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CategoryView;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CategoryQueryService implements GetCategoriesQuery {

    private final CategoryPersistencePort categoryPersistencePort;

    public CategoryQueryService(CategoryPersistencePort categoryPersistencePort) {
        this.categoryPersistencePort = categoryPersistencePort;
    }

    @Override
    public List<CategoryView> getByOwner(UserId ownerId) {
        List<Category> categories = categoryPersistencePort.findByOwner(ownerId);
        return toViews(categories);
    }

    @Override
    public CategoryView getById(CategoryId categoryId, UserId ownerId) {
        Category category = categoryPersistencePort.findById(categoryId, ownerId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId.value()));
        List<Category> all = categoryPersistencePort.findByOwner(ownerId);
        Map<Long, String> nameMap = buildNameMap(all);
        return toView(category, nameMap);
    }

    @Override
    public List<CategoryView> getByType(UserId ownerId, String typeCode) {
        List<Category> categories = categoryPersistencePort.findByOwnerAndType(ownerId, typeCode);
        return toViews(categories);
    }

    private List<CategoryView> toViews(List<Category> categories) {
        Map<Long, String> nameMap = buildNameMap(categories);
        return categories.stream()
                .map(c -> toView(c, nameMap))
                .toList();
    }

    private Map<Long, String> buildNameMap(List<Category> categories) {
        return categories.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(c -> c.getId().value(), Category::getName));
    }

    private CategoryView toView(Category category, Map<Long, String> nameMap) {
        Long parentId = category.getParentCategoryId() != null ? category.getParentCategoryId().value() : null;
        String parentName = parentId != null ? nameMap.get(parentId) : null;
        return new CategoryView(
                category.getId().value(), category.getName(),
                category.getCategoryType().getCode(), category.getCategoryType().getName(),
                parentId, parentName,
                category.getIcon(), category.getColor(),
                category.isSystem(), category.isActive(),
                category.getAuditInfo() != null ? category.getAuditInfo().createdAt() : null);
    }
}
