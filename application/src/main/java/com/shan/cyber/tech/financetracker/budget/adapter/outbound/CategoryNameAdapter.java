package com.shan.cyber.tech.financetracker.budget.adapter.outbound;

import com.shan.cyber.tech.financetracker.budget.domain.port.outbound.CategoryNameQueryPort;
import com.shan.cyber.tech.financetracker.category.domain.port.inbound.GetCategoriesQuery;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

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
}
