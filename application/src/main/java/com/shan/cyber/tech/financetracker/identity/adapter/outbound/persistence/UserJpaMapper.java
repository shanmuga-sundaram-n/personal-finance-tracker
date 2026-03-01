package com.shan.cyber.tech.financetracker.identity.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

@Component
public class UserJpaMapper {

    public User toDomain(UserJpaEntity entity) {
        AuditInfo auditInfo = new AuditInfo(
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy());

        return new User(
                new UserId(entity.getId()),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isActive(),
                entity.getPreferredCurrency(),
                auditInfo);
    }

    public UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        if (user.getId() != null) {
            entity.setId(user.getId().value());
        }
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setEmail(user.getEmail());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setActive(user.isActive());
        entity.setPreferredCurrency(user.getPreferredCurrency());
        return entity;
    }
}
