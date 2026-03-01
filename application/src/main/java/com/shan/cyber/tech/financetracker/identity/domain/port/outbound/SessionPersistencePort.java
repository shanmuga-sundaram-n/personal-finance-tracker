package com.shan.cyber.tech.financetracker.identity.domain.port.outbound;

import com.shan.cyber.tech.financetracker.identity.domain.model.Session;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SessionPersistencePort {
    Optional<Session> findValidSession(String token, OffsetDateTime now);
    Session save(Session session);
    void deleteByToken(String token);
    void deleteExpiredBefore(OffsetDateTime cutoff);
}
