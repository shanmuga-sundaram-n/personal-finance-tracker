package com.shan.cyber.tech.financetracker.category.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {
    public CategoryNotFoundException(Long categoryId) {
        super("Category", categoryId);
    }
}
