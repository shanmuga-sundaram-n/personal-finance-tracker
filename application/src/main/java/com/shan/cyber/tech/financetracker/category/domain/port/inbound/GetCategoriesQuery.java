package com.shan.cyber.tech.financetracker.category.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;

public interface GetCategoriesQuery {
    List<CategoryView> getByOwner(UserId ownerId);
    CategoryView getById(CategoryId categoryId, UserId ownerId);
    List<CategoryView> getByType(UserId ownerId, String typeCode);
}
