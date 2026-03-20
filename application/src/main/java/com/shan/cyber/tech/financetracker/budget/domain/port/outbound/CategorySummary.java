package com.shan.cyber.tech.financetracker.budget.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;

public record CategorySummary(CategoryId id, String name, String typeCode, Long parentCategoryId) {}
