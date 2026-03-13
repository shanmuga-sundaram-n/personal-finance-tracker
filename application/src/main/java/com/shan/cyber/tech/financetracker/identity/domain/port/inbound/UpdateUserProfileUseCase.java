package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

public interface UpdateUserProfileUseCase {
    UserProfile updateProfile(UpdateUserProfileCommand command);
}
