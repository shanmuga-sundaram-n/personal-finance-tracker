package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record CreateAccountCommand(
        UserId ownerId,
        String name,
        String accountTypeCode,
        Money initialBalance,
        String institutionName,
        String accountNumberLast4) {
}
