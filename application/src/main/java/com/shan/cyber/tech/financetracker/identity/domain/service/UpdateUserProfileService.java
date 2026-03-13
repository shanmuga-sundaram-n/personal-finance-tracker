package com.shan.cyber.tech.financetracker.identity.domain.service;

import com.shan.cyber.tech.financetracker.identity.domain.exception.UserNotFoundException;
import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.UserPersistencePort;

public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final UserPersistencePort userPersistencePort;

    public UpdateUserProfileService(UserPersistencePort userPersistencePort) {
        this.userPersistencePort = userPersistencePort;
    }

    @Override
    public UserProfile updateProfile(UpdateUserProfileCommand command) {
        User user = userPersistencePort.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + command.userId().value() + " not found"));

        user.updateProfile(command.firstName(), command.lastName(), command.preferredCurrency());
        User saved = userPersistencePort.save(user);

        return new UserProfile(
                saved.getId().value(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getPreferredCurrency(),
                saved.getAuditInfo() != null ? saved.getAuditInfo().createdAt() : null
        );
    }
}
