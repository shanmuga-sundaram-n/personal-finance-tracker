package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;

public interface CategoryNameQueryPort {

    String getCategoryName(CategoryId categoryId, UserId userId);

    List<CategorySummary> getCategoriesVisibleToUser(UserId userId);
}
