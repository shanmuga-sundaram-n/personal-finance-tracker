package com.shan.cyber.tech.financetracker.transaction.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface BalanceUpdatePort {

    void debit(AccountId accountId, UserId userId, Money amount);

    void credit(AccountId accountId, UserId userId, Money amount);

    void reverseDebit(AccountId accountId, UserId userId, Money amount);

    void reverseCredit(AccountId accountId, UserId userId, Money amount);

    boolean canDebit(AccountId accountId, UserId userId, Money amount);
}
