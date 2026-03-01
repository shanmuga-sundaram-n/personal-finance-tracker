package com.shan.cyber.tech.financetracker.account.domain.port.inbound;

public interface UpdateAccountUseCase {
    AccountView updateAccount(UpdateAccountCommand command);
}
