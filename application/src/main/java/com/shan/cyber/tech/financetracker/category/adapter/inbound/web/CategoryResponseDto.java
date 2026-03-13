package com.shan.cyber.tech.financetracker.category.adapter.inbound.web;

import java.time.OffsetDateTime;

public record CategoryResponseDto(
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
