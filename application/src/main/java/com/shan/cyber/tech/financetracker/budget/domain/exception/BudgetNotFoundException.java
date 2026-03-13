package com.shan.cyber.tech.financetracker.budget.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.ResourceNotFoundException;

public class BudgetNotFoundException extends ResourceNotFoundException {

    public BudgetNotFoundException(Long id) {
        super("Budget", id);
    }
}
