package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public interface RegisterUserUseCase {
    UserId registerUser(RegisterUserCommand command);
}
