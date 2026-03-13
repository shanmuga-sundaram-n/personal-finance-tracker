package com.shan.cyber.tech.financetracker.reporting.domain.port.outbound;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.AccountBalanceSummary;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.List;

public interface AccountQueryPort {

    NetWorthSummary getNetWorth(UserId userId);

    List<AccountBalanceSummary> getAccountBalances(UserId userId);
}
