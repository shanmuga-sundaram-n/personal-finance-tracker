package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.category.domain.model.CategoryType;
import com.shan.cyber.tech.financetracker.category.domain.port.outbound.CategoryTypePersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryTypePersistenceAdapter implements CategoryTypePersistencePort {

    private final CategoryTypeJpaRepository repository;
    private final CategoryTypeJpaMapper mapper;

    public CategoryTypePersistenceAdapter(CategoryTypeJpaRepository repository, CategoryTypeJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CategoryType> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<CategoryType> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
