package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record UpdateCategoryCommand(
        CategoryId categoryId,
        UserId ownerId,
        String name,
        String icon,
        String color) {
}
