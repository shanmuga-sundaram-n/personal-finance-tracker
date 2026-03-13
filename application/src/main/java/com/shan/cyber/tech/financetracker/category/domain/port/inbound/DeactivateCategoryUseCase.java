package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface DeactivateCategoryUseCase {
    void deactivateCategory(CategoryId categoryId, UserId requestingUser);
}
