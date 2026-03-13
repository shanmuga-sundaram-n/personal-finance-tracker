package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record CreateCategoryCommand(
        UserId ownerId,
        String categoryTypeCode,
        CategoryId parentCategoryId,
        String name,
        String icon,
        String color) {
}
