package com.shan.cyber.tech.financetracker.account.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;

public class InsufficientFundsException extends DomainException {

    private final AccountId accountId;
    private final Money resultingBalance;

    public InsufficientFundsException(AccountId accountId, Money resultingBalance) {
        super("INSUFFICIENT_FUNDS", "Insufficient funds in account " + accountId.value()
                + ". Resulting balance would be " + resultingBalance);
        this.accountId = accountId;
        this.resultingBalance = resultingBalance;
    }

    public AccountId getAccountId() { return accountId; }
    public Money getResultingBalance() { return resultingBalance; }
}
