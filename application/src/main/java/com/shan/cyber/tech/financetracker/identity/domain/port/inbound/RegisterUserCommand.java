package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

public record RegisterUserCommand(
        String username,
        String email,
        String rawPassword,
        String firstName,
        String lastName) {
}
