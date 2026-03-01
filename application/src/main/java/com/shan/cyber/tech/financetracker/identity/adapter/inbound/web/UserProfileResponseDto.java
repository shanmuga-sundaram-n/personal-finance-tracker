package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import java.time.OffsetDateTime;

public record UserProfileResponseDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String preferredCurrency,
        OffsetDateTime createdAt) {
}
