package com.shan.cyber.tech.financetracker.identity.domain.service;

import com.shan.cyber.tech.financetracker.identity.domain.exception.UserNotFoundException;
import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.GetCurrentUserQuery;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.UserPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public class IdentityQueryService implements GetCurrentUserQuery {

    private final UserPersistencePort userPersistencePort;

    public IdentityQueryService(UserPersistencePort userPersistencePort) {
        this.userPersistencePort = userPersistencePort;
    }

    @Override
    public UserProfile getCurrentUser(UserId userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId.value() + " not found"));

        return new UserProfile(
                user.getId().value(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPreferredCurrency(),
                user.getAuditInfo() != null ? user.getAuditInfo().createdAt() : null
        );
    }
}
