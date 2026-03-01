package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

public interface AuthenticateUserUseCase {
    LoginResult authenticate(AuthenticateUserCommand command);
}
