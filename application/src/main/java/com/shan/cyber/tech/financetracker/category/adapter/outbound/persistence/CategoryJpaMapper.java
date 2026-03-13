package com.shan.cyber.tech.financetracker.category.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.category.domain.model.Category;
import com.shan.cyber.tech.financetracker.category.domain.model.CategoryType;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class CategoryJpaMapper {

    private final CategoryTypeJpaMapper categoryTypeMapper;

    public CategoryJpaMapper(CategoryTypeJpaMapper categoryTypeMapper) {
        this.categoryTypeMapper = categoryTypeMapper;
    }

    public Category toDomain(CategoryJpaEntity entity) {
        CategoryType categoryType = categoryTypeMapper.toDomain(entity.getCategoryType());
        AuditInfo auditInfo = new AuditInfo(
                entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getCreatedBy(), entity.getUpdatedBy());

        return new Category(
                new CategoryId(entity.getId()),
                entity.getUserId() != null ? new UserId(entity.getUserId()) : null,
                categoryType,
                entity.getParentCategoryId() != null ? new CategoryId(entity.getParentCategoryId()) : null,
                entity.getName(),
                entity.getIcon(),
                entity.getColor(),
                entity.isSystem(),
                entity.isActive(),
                auditInfo);
    }

    public CategoryJpaEntity toJpaEntity(Category category, CategoryTypeJpaEntity categoryTypeEntity) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        if (category.getId() != null) {
            entity.setId(category.getId().value());
        }
        entity.setUserId(category.getOwnerId() != null ? category.getOwnerId().value() : null);
        entity.setCategoryType(categoryTypeEntity);
        entity.setParentCategoryId(category.getParentCategoryId() != null ? category.getParentCategoryId().value() : null);
        entity.setName(category.getName());
        entity.setIcon(category.getIcon());
        entity.setColor(category.getColor());
        entity.setSystem(category.isSystem());
        entity.setActive(category.isActive());
        return entity;
    }
}
