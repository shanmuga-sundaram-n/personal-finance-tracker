package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;

public interface CreateAccountUseCase {
    AccountId createAccount(CreateAccountCommand command);
}
