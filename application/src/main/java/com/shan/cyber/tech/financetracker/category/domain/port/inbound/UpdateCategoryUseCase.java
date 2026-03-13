package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

public interface UpdateCategoryUseCase {
    CategoryView updateCategory(UpdateCategoryCommand command);
}
