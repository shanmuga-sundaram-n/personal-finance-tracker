package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

public record AuthenticateUserCommand(
        String username,
        String rawPassword,
        String clientIp) {
}
