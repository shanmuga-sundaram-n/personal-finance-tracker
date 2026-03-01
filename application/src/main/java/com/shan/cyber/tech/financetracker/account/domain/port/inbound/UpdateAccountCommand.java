package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record UpdateAccountCommand(
        AccountId accountId,
        UserId ownerId,
        String name,
        String institutionName) {
}
