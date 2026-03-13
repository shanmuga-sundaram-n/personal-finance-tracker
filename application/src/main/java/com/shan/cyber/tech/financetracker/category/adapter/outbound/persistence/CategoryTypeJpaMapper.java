package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.category.domain.model.CategoryType;
import org.springframework.stereotype.Component;

@Component
public class CategoryTypeJpaMapper {

    public CategoryType toDomain(CategoryTypeJpaEntity entity) {
        return new CategoryType(entity.getId(), entity.getCode(), entity.getName());
    }
}
