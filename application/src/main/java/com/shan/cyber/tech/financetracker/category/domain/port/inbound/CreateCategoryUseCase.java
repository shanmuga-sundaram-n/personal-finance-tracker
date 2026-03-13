package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;

public interface CreateCategoryUseCase {
    CategoryId createCategory(CreateCategoryCommand command);
}
