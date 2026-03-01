package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import java.time.OffsetDateTime;

public record LoginResponseDto(String token, OffsetDateTime expiresAt) {
}
