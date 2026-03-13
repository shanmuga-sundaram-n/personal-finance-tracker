package com.shan.cyber.tech.financetracker.category.domain.port.outbound;

import com.shan.cyber.tech.financetracker.category.domain.model.Category;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface CategoryPersistencePort {
    Optional<Category> findById(CategoryId id, UserId ownerId);
    List<Category> findByOwner(UserId ownerId);
    List<Category> findByOwnerAndType(UserId ownerId, String typeCode);
    Optional<Category> findByOwnerAndName(UserId ownerId, String name, CategoryId parentCategoryId);
    Category save(Category category);
}
