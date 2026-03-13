package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public record UpdateUserProfileCommand(UserId userId, String firstName, String lastName, String preferredCurrency) {
}
