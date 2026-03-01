package com.shan.cyber.tech.financetracker.identity.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.identity.domain.model.Session;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.SessionPersistencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class SessionPersistenceAdapter implements SessionPersistencePort {

    private final SessionJpaRepository repository;
    private final SessionJpaMapper mapper;

    public SessionPersistenceAdapter(SessionJpaRepository repository, SessionJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Session> findValidSession(String token, OffsetDateTime now) {
        return repository.findValidSession(token, now).map(mapper::toDomain);
    }

    @Override
    public Session save(Session session) {
        SessionJpaEntity entity = mapper.toJpaEntity(session);
        SessionJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void deleteExpiredBefore(OffsetDateTime cutoff) {
        repository.deleteByExpiresAtBefore(cutoff);
    }
}
