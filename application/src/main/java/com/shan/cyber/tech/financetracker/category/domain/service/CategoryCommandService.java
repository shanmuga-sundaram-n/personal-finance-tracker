package com.shan.cyber.tech.financetracker.category.domain.service;

import com.shan.cyber.tech.financetracker.category.domain.event.CategoryCreated;
import com.shan.cyber.tech.financetracker.category.domain.event.CategoryDeactivated;
import com.shan.cyber.tech.financetracker.category.domain.exception.CategoryNotFoundException;
import com.shan.cyber.tech.financetracker.category.domain.exception.DuplicateCategoryNameException;
import com.shan.cyber.tech.financetracker.category.domain.exception.SystemCategoryModificationException;
import com.shan.cyber.tech.financetracker.category.domain.model.Category;
import com.shan.cyber.tech.financetracker.category.domain.model.CategoryType;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CategoryView;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.DeactivateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryUseCase;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryEventPublisherPort;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryPersistencePort;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryTypePersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public class CategoryCommandService implements CreateCategoryUseCase, UpdateCategoryUseCase,
        DeactivateCategoryUseCase {

    private final CategoryPersistencePort categoryPersistencePort;
    private final CategoryTypePersistencePort categoryTypePersistencePort;
    private final CategoryEventPublisherPort eventPublisherPort;

    public CategoryCommandService(CategoryPersistencePort categoryPersistencePort,
                                   CategoryTypePersistencePort categoryTypePersistencePort,
                                   CategoryEventPublisherPort eventPublisherPort) {
        this.categoryPersistencePort = categoryPersistencePort;
        this.categoryTypePersistencePort = categoryTypePersistencePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public CategoryId createCategory(CreateCategoryCommand command) {
        CategoryType categoryType = categoryTypePersistencePort.findByCode(command.categoryTypeCode())
                .orElseThrow(() -> new BusinessRuleException("INVALID_CATEGORY_TYPE",
                        "Invalid category type code: " + command.categoryTypeCode()));

        if (command.parentCategoryId() != null) {
            categoryPersistencePort.findById(command.parentCategoryId(), command.ownerId())
                    .orElseThrow(() -> new CategoryNotFoundException(command.parentCategoryId().value()));
        }

        categoryPersistencePort.findByOwnerAndName(command.ownerId(), command.name(), command.parentCategoryId())
                .filter(Category::isActive)
                .ifPresent(existing -> { throw new DuplicateCategoryNameException(command.name()); });

        Category category = Category.create(command.ownerId(), categoryType,
                command.parentCategoryId(), command.name(), command.icon(), command.color());

        Category saved = categoryPersistencePort.save(category);
        eventPublisherPort.publish(new CategoryCreated(saved.getId(), saved.getOwnerId(), saved.getName()));

        return saved.getId();
    }

    @Override
    public CategoryView updateCategory(UpdateCategoryCommand command) {
        Category category = findCategoryOrThrow(command.categoryId(), command.ownerId());

        if (category.isSystem()) {
            throw new SystemCategoryModificationException();
        }

        if (!category.getName().equalsIgnoreCase(command.name())) {
            categoryPersistencePort.findByOwnerAndName(command.ownerId(), command.name(), category.getParentCategoryId())
                    .filter(Category::isActive)
                    .ifPresent(existing -> { throw new DuplicateCategoryNameException(command.name()); });
        }

        category.rename(command.name());
        category.updateIcon(command.icon());
        category.updateColor(command.color());

        Category saved = categoryPersistencePort.save(category);
        return toView(saved);
    }

    @Override
    public void deactivateCategory(CategoryId categoryId, UserId requestingUser) {
        Category category = findCategoryOrThrow(categoryId, requestingUser);

        if (category.isSystem()) {
            throw new SystemCategoryModificationException();
        }

        category.deactivate();
        categoryPersistencePort.save(category);
        eventPublisherPort.publish(new CategoryDeactivated(categoryId, requestingUser));
    }

    private Category findCategoryOrThrow(CategoryId categoryId, UserId ownerId) {
        return categoryPersistencePort.findById(categoryId, ownerId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId.value()));
    }

    private CategoryView toView(Category category) {
        return new CategoryView(
                category.getId().value(), category.getName(),
                category.getCategoryType().getCode(), category.getCategoryType().getName(),
                category.getParentCategoryId() != null ? category.getParentCategoryId().value() : null,
                null,
                category.getIcon(), category.getColor(),
                category.isSystem(), category.isActive(),
                category.getAuditInfo() != null ? category.getAuditInfo().createdAt() : null);
    }
}
