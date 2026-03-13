package com.shan.cyber.tech.financetracker.category.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DuplicateResourceException;

public class DuplicateCategoryNameException extends DuplicateResourceException {
    public DuplicateCategoryNameException(String name) {
        super("Category", "name", name);
    }
}
