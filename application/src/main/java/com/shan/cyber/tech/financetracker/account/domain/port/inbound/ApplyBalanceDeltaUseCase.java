package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface ApplyBalanceDeltaUseCase {
    void applyDebit(AccountId accountId, UserId userId, Money amount);
    void applyCredit(AccountId accountId, UserId userId, Money amount);
    void reverseDebit(AccountId accountId, UserId userId, Money amount);
    void reverseCredit(AccountId accountId, UserId userId, Money amount);
    boolean canDebit(AccountId accountId, UserId userId, Money amount);
}
