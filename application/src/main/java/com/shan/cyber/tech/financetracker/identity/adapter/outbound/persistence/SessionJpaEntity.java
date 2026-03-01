package com.shan.cyber.tech.financetracker.identity.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.shared.adapter.outbound.persistence.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(schema = "finance_tracker", name = "sessions")
public class SessionJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sessions_id_gen")
    @SequenceGenerator(name = "sessions_id_gen", sequenceName = "finance_tracker.sessions_id_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected SessionJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
