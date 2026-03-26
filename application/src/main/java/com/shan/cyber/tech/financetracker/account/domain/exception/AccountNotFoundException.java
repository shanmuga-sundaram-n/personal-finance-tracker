package com.shan.cyber.tech.financetracker.account.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.ResourceNotFoundException;

/**
 * Thrown when an account is not found by id+userId. This covers two scenarios:
 * 1. The account does not exist at all.
 * 2. The account is soft-deleted (is_active = false) — the persistence adapter
 *    uses findByIdAndUserIdAndIsActiveTrue which returns Optional.empty() for
 *    inactive accounts, producing this exception.
 *
 * Extends ResourceNotFoundException so GlobalExceptionHandler maps it to HTTP 404.
 */
public class AccountNotFoundException extends ResourceNotFoundException {
    public AccountNotFoundException(Long accountId) {
        super("Account", accountId);
    }
}
