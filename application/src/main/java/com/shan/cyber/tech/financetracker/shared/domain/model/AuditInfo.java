package com.shan.cyber.tech.financetracker.shared.domain.model;

import java.time.OffsetDateTime;

public record AuditInfo(
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long createdBy,
        Long updatedBy) {
}
