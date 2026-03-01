package com.shan.cyber.tech.financetracker.identity.domain.port.inbound;

import java.time.OffsetDateTime;

public record LoginResult(String token, OffsetDateTime expiresAt) {
}
