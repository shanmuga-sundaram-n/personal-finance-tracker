package com.shan.cyber.tech.financetracker.identity.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.identity.domain.model.Session;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class SessionJpaMapper {

    public Session toDomain(SessionJpaEntity entity) {
        AuditInfo auditInfo = new AuditInfo(
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy());

        return new Session(
                entity.getId(),
                new UserId(entity.getUserId()),
                entity.getToken(),
                entity.getExpiresAt(),
                auditInfo);
    }

    public SessionJpaEntity toJpaEntity(Session session) {
        SessionJpaEntity entity = new SessionJpaEntity();
        if (session.getId() != null) {
            entity.setId(session.getId());
        }
        entity.setUserId(session.getUserId().value());
        entity.setToken(session.getToken());
        entity.setExpiresAt(session.getExpiresAt());
        return entity;
    }
}
