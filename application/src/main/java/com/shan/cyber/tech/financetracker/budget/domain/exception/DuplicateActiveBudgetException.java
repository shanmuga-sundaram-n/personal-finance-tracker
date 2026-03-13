package com.shan.cyber.tech.financetracker.budget.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.BusinessRuleException;

public class DuplicateActiveBudgetException extends BusinessRuleException {

    public DuplicateActiveBudgetException() {
        super("DUPLICATE_ACTIVE_BUDGET",
                "An active budget already exists for this category and period type");
    }
}
