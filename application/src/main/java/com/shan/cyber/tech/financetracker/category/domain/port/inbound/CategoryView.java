package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

import java.time.OffsetDateTime;

public record CategoryView(
        Long id,
        String name,
        String categoryTypeCode,
        String categoryTypeName,
        Long parentCategoryId,
        String parentCategoryName,
        String icon,
        String color,
        boolean isSystem,
        boolean isActive,
        OffsetDateTime createdAt) {
}
