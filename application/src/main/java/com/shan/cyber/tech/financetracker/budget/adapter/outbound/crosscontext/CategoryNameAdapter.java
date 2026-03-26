package com.shan.cyber.tech.financetracker.budget.adapter.outbound.crosscontext;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategorySummary;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryNameAdapter implements CategoryNameQueryPort {

    private final GetCategoriesQuery getCategoriesQuery;

    public CategoryNameAdapter(GetCategoriesQuery getCategoriesQuery) {
        this.getCategoriesQuery = getCategoriesQuery;
    }

    @Override
    public String getCategoryName(CategoryId categoryId, UserId userId) {
        return getCategoriesQuery.getById(categoryId, userId).name();
    }

    @Override
    public List<CategorySummary> getCategoriesVisibleToUser(UserId userId) {
        return getCategoriesQuery.getByOwner(userId).stream()
                .map(cv -> new CategorySummary(new CategoryId(cv.id()), cv.name(), cv.categoryTypeCode(), cv.parentCategoryId()))
                .toList();
    }
}
