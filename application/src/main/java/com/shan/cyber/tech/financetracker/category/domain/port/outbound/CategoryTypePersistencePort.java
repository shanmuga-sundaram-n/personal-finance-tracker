package com.shan.cyber.tech.financetracker.category.domain.port.outbound;

import com.shan.cyber.tech.financetracker.category.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;

public interface CategoryTypePersistencePort {
    Optional<CategoryType> findByCode(String code);
    List<CategoryType> findAll();
}
