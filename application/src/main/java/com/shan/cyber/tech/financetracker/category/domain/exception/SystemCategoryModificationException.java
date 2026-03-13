package com.shan.cyber.tech.financetracker.category.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.ForbiddenOperationException;

public class SystemCategoryModificationException extends ForbiddenOperationException {
    public SystemCategoryModificationException() {
        super("System categories cannot be modified or deleted");
    }
}
