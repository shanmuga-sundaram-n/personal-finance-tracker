package com.shan.cyber.tech.financetracker.account.domain.port.outbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;

public interface AccountTransactionCountPort {
    boolean hasTransactions(AccountId accountId);
}
