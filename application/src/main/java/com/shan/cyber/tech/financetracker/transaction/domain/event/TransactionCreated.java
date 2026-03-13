package com.shan.cyber.tech.financetracker.transaction.domain.event;

import com.shan.cyber.tech.financetracker.shared.domain.event.DomainEvent;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;

public record TransactionCreated(
        TransactionId transactionId,
        UserId userId,
        AccountId accountId,
        Money amount,
        TransactionType type) implements DomainEvent {
}
