package com.shan.cyber.tech.financetracker.transaction.adapter.outbound;

import com.shan.cyber.tech.financetracker.account.domain.port.inbound.ApplyBalanceDeltaUseCase;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.BalanceUpdatePort;
import org.springframework.stereotype.Component;

@Component
public class AccountBalanceAdapter implements BalanceUpdatePort {

    private final ApplyBalanceDeltaUseCase applyBalanceDeltaUseCase;

    public AccountBalanceAdapter(ApplyBalanceDeltaUseCase applyBalanceDeltaUseCase) {
        this.applyBalanceDeltaUseCase = applyBalanceDeltaUseCase;
    }

    @Override
    public void debit(AccountId accountId, UserId userId, Money amount) {
        applyBalanceDeltaUseCase.applyDebit(accountId, userId, amount);
    }

    @Override
    public void credit(AccountId accountId, UserId userId, Money amount) {
        applyBalanceDeltaUseCase.applyCredit(accountId, userId, amount);
    }

    @Override
    public void reverseDebit(AccountId accountId, UserId userId, Money amount) {
        applyBalanceDeltaUseCase.reverseDebit(accountId, userId, amount);
    }

    @Override
    public void reverseCredit(AccountId accountId, UserId userId, Money amount) {
        applyBalanceDeltaUseCase.reverseCredit(accountId, userId, amount);
    }

    @Override
    public boolean canDebit(AccountId accountId, UserId userId, Money amount) {
        return applyBalanceDeltaUseCase.canDebit(accountId, userId, amount);
    }
}
