package com.shan.cyber.tech.financetracker.category.domain.service;

import com.shan.cyber.tech.financetracker.category.domain.exception.CategoryNotFoundException;
import com.shan.cyber.tech.financetracker.category.domain.exception.DuplicateCategoryNameException;
import com.shan.cyber.tech.financetracker.category.domain.exception.SystemCategoryModificationException;
import com.shan.cyber.tech.financetracker.category.domain.model.Category;
import com.shan.cyber.tech.financetracker.category.domain.model.CategoryType;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.CreateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.UpdateCategoryCommand;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryEventPublisherPort;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryPersistencePort;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryTypePersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

    @Mock private CategoryPersistencePort categoryPersistencePort;
    @Mock private CategoryTypePersistencePort categoryTypePersistencePort;
    @Mock private CategoryEventPublisherPort eventPublisherPort;

    private CategoryCommandService service;

    private static final UserId OWNER = new UserId(1L);
    private static final CategoryType EXPENSE_TYPE = new CategoryType((short) 2, "EXPENSE", "Expense");

    @BeforeEach
    void setUp() {
        service = new CategoryCommandService(categoryPersistencePort, categoryTypePersistencePort, eventPublisherPort);
    }

    @Test
    void createCategory_success_returnsCategoryId() {
        CreateCategoryCommand command = new CreateCategoryCommand(OWNER, "EXPENSE", null, "Groceries", null, null);

        Category saved = Category.create(OWNER, EXPENSE_TYPE, null, "Groceries", null, null);
        saved.setId(new CategoryId(10L));

        when(categoryTypePersistencePort.findByCode("EXPENSE")).thenReturn(Optional.of(EXPENSE_TYPE));
        when(categoryPersistencePort.findByOwnerAndName(OWNER, "Groceries", null)).thenReturn(Optional.empty());
        when(categoryPersistencePort.save(any(Category.class))).thenReturn(saved);

        CategoryId result = service.createCategory(command);

        assertNotNull(result);
        assertEquals(10L, result.value());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void createCategory_invalidCategoryType_throwsBusinessRuleException() {
        CreateCategoryCommand command = new CreateCategoryCommand(OWNER, "INVALID_TYPE", null, "Test", null, null);
        when(categoryTypePersistencePort.findByCode("INVALID_TYPE")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.createCategory(command));
        verify(categoryPersistencePort, never()).save(any());
    }

    @Test
    void createCategory_duplicateName_throwsDuplicateCategoryNameException() {
        CreateCategoryCommand command = new CreateCategoryCommand(OWNER, "EXPENSE", null, "Groceries", null, null);
        Category existing = Category.create(OWNER, EXPENSE_TYPE, null, "Groceries", null, null);
        existing.setId(new CategoryId(5L));

        when(categoryTypePersistencePort.findByCode("EXPENSE")).thenReturn(Optional.of(EXPENSE_TYPE));
        when(categoryPersistencePort.findByOwnerAndName(OWNER, "Groceries", null)).thenReturn(Optional.of(existing));

        assertThrows(DuplicateCategoryNameException.class, () -> service.createCategory(command));
        verify(categoryPersistencePort, never()).save(any());
    }

    @Test
    void createCategory_parentNotFound_throwsCategoryNotFoundException() {
        CategoryId parentId = new CategoryId(99L);
        CreateCategoryCommand command = new CreateCategoryCommand(OWNER, "EXPENSE", parentId, "Sub", null, null);

        when(categoryTypePersistencePort.findByCode("EXPENSE")).thenReturn(Optional.of(EXPENSE_TYPE));
        when(categoryPersistencePort.findById(parentId, OWNER)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.createCategory(command));
    }

    @Test
    void updateCategory_systemCategory_throwsSystemCategoryModificationException() {
        CategoryId categoryId = new CategoryId(1L);
        // Create a system category (isSystem = true)
        Category systemCategory = new Category(categoryId, null, EXPENSE_TYPE, null,
                "Food", null, null, true, true, null);

        UpdateCategoryCommand command = new UpdateCategoryCommand(categoryId, OWNER, "Food Renamed", null, null);

        when(categoryPersistencePort.findById(categoryId, OWNER)).thenReturn(Optional.of(systemCategory));

        assertThrows(SystemCategoryModificationException.class, () -> service.updateCategory(command));
        verify(categoryPersistencePort, never()).save(any());
    }

    @Test
    void updateCategory_duplicateName_throwsDuplicateCategoryNameException() {
        CategoryId categoryId = new CategoryId(1L);
        Category category = Category.create(OWNER, EXPENSE_TYPE, null, "Groceries", null, null);
        category.setId(categoryId);

        Category duplicate = Category.create(OWNER, EXPENSE_TYPE, null, "Shopping", null, null);
        duplicate.setId(new CategoryId(2L));

        UpdateCategoryCommand command = new UpdateCategoryCommand(categoryId, OWNER, "Shopping", null, null);

        when(categoryPersistencePort.findById(categoryId, OWNER)).thenReturn(Optional.of(category));
        when(categoryPersistencePort.findByOwnerAndName(OWNER, "Shopping", null)).thenReturn(Optional.of(duplicate));

        assertThrows(DuplicateCategoryNameException.class, () -> service.updateCategory(command));
    }

    @Test
    void deactivateCategory_success_deactivatesAndPublishesEvent() {
        CategoryId categoryId = new CategoryId(1L);
        Category category = Category.create(OWNER, EXPENSE_TYPE, null, "Groceries", null, null);
        category.setId(categoryId);

        when(categoryPersistencePort.findById(categoryId, OWNER)).thenReturn(Optional.of(category));

        service.deactivateCategory(categoryId, OWNER);

        assertFalse(category.isActive());
        verify(categoryPersistencePort).save(category);
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void deactivateCategory_systemCategory_throwsSystemCategoryModificationException() {
        CategoryId categoryId = new CategoryId(1L);
        Category systemCategory = new Category(categoryId, null, EXPENSE_TYPE, null,
                "Food", null, null, true, true, null);

        when(categoryPersistencePort.findById(categoryId, OWNER)).thenReturn(Optional.of(systemCategory));

        assertThrows(SystemCategoryModificationException.class,
                () -> service.deactivateCategory(categoryId, OWNER));
        verify(categoryPersistencePort, never()).save(any());
    }

    @Test
    void deactivateCategory_notFound_throwsCategoryNotFoundException() {
        CategoryId categoryId = new CategoryId(999L);
        when(categoryPersistencePort.findById(categoryId, OWNER)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
                () -> service.deactivateCategory(categoryId, OWNER));
    }
}
