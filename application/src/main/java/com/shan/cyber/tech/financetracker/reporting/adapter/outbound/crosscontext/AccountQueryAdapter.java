package com.shan.cyber.tech.financetracker.reporting.adapter.outbound.crosscontext;

import com.shan.cyber.tech.financetracker.account.domain.port.inbound.AccountView;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.GetAccountsQuery;
import com.shan.cyber.tech.financetracker.account.domain.port.inbound.NetWorthView;
import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.AccountBalanceSummary;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.AccountQueryPort;
import com.shan.cyber.tech.financetracker.reporting.domain.port.outbound.NetWorthSummary;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountQueryAdapter implements AccountQueryPort {

    private final GetAccountsQuery getAccountsQuery;

    public AccountQueryAdapter(GetAccountsQuery getAccountsQuery) {
        this.getAccountsQuery = getAccountsQuery;
    }

    @Override
    public NetWorthSummary getNetWorth(UserId userId) {
        NetWorthView view = getAccountsQuery.getNetWorth(userId);
        return new NetWorthSummary(
                view.totalAssets().amount().toPlainString(),
                view.totalLiabilities().amount().toPlainString(),
                view.netWorth().amount().toPlainString(),
                view.netWorth().currency());
    }

    @Override
    public List<AccountBalanceSummary> getAccountBalances(UserId userId) {
        return getAccountsQuery.getAccountsByOwner(userId).stream()
                .filter(AccountView::isActive)
                .map(a -> new AccountBalanceSummary(
                        a.id(),
                        a.name(),
                        a.currentBalance().amount().toPlainString(),
                        a.currency(),
                        a.isLiability()))
                .toList();
    }
}
