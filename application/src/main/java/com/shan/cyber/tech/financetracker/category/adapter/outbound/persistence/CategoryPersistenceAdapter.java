package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.category.domain.model.Category;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryPersistenceAdapter implements CategoryPersistencePort {

    private final CategoryJpaRepository repository;
    private final CategoryTypeJpaRepository categoryTypeRepository;
    private final CategoryJpaMapper mapper;

    public CategoryPersistenceAdapter(CategoryJpaRepository repository,
                                       CategoryTypeJpaRepository categoryTypeRepository,
                                       CategoryJpaMapper mapper) {
        this.repository = repository;
        this.categoryTypeRepository = categoryTypeRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Category> findById(CategoryId id, UserId ownerId) {
        return repository.findByIdForUser(id.value(), ownerId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Category> findByOwner(UserId ownerId) {
        return repository.findSystemAndUserCategories(ownerId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Category> findByOwnerAndType(UserId ownerId, String typeCode) {
        return repository.findSystemAndUserCategoriesByType(ownerId.value(), typeCode).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findByOwnerAndName(UserId ownerId, String name, CategoryId parentCategoryId) {
        Long parentId = parentCategoryId != null ? parentCategoryId.value() : null;
        return repository.findByNameForUser(ownerId.value(), name, parentId)
                .map(mapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        CategoryTypeJpaEntity categoryTypeEntity = categoryTypeRepository
                .findByCode(category.getCategoryType().getCode())
                .orElseThrow();
        CategoryJpaEntity entity = mapper.toJpaEntity(category, categoryTypeEntity);
        CategoryJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
