package com.shan.cyber.tech.financetracker.identity.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.UserPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserJpaRepository repository;
    private final UserJpaMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repository, UserJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toJpaEntity(user);
        UserJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
