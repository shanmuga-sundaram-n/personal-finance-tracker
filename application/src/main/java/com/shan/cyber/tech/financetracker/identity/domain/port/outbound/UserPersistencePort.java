package com.shan.cyber.tech.financetracker.identity.domain.port.outbound;

import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.Optional;

public interface UserPersistencePort {
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
}
