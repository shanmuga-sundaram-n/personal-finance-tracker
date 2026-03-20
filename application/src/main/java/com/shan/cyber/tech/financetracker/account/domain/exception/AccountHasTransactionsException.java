package com.shan.cyber.tech.financetracker.account.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class AccountHasTransactionsException extends DomainException {

    public AccountHasTransactionsException(Long accountId) {
        super("ACCOUNT_HAS_TRANSACTIONS",
              "Account " + accountId + " cannot be deleted because it has existing transactions");
    }
}
