package com.shan.cyber.tech.financetracker.identity.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class Session {

    private Long id;
    private UserId userId;
    private String token;
    private OffsetDateTime expiresAt;
    private AuditInfo auditInfo;

    public Session(Long id, UserId userId, String token, OffsetDateTime expiresAt, AuditInfo auditInfo) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.auditInfo = auditInfo;
    }

    public static Session create(UserId userId, int durationDays) {
        String token = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(durationDays);
        return new Session(null, userId, token, expiresAt, null);
    }

    public boolean isExpired(OffsetDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isValid(OffsetDateTime now) {
        return !isExpired(now);
    }

    public Long getId() { return id; }
    public UserId getUserId() { return userId; }
    public String getToken() { return token; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public AuditInfo getAuditInfo() { return auditInfo; }

    public void setId(Long id) { this.id = id; }
    public void setAuditInfo(AuditInfo auditInfo) { this.auditInfo = auditInfo; }
}
