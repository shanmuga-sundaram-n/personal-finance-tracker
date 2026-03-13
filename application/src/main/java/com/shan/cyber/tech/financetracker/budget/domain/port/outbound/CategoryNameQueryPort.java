package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface CategoryNameQueryPort {

    String getCategoryName(CategoryId categoryId, UserId userId);
}
