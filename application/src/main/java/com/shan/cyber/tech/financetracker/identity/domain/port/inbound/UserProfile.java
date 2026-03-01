package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

import java.time.OffsetDateTime;

public record UserProfile(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String preferredCurrency,
        OffsetDateTime createdAt) {
}
