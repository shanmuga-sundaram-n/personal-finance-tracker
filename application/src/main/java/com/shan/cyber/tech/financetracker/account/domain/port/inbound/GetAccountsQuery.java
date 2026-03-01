package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;

public interface GetAccountsQuery {
    List<AccountView> getAccountsByOwner(UserId ownerId);
    AccountView getAccountById(AccountId accountId, UserId ownerId);
    NetWorthView getNetWorth(UserId ownerId);
}
