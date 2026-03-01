package com.shan.cyber.tech.financetracker.identity.domain.service;

import com.shan.cyber.tech.financetracker.identity.domain.event.UserRegistered;
import com.shan.cyber.tech.financetracker.identity.domain.exception.DuplicateEmailException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.DuplicateUsernameException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.InvalidCredentialsException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.RateLimitExceededException;
import com.shan.cyber.tech.financetracker.identity.domain.model.Session;
import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.AuthenticateUserCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.AuthenticateUserUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LoginResult;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LogoutUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.RegisterUserCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.RegisterUserUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.IdentityEventPublisherPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.LoginRateLimiterPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.PasswordHasherPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.SessionPersistencePort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.UserPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

public class IdentityCommandService implements RegisterUserUseCase, AuthenticateUserUseCase, LogoutUseCase {

    private final UserPersistencePort userPersistencePort;
    private final SessionPersistencePort sessionPersistencePort;
    private final PasswordHasherPort passwordHasherPort;
    private final LoginRateLimiterPort loginRateLimiterPort;
    private final IdentityEventPublisherPort eventPublisherPort;
    private final int sessionDurationDays;

    public IdentityCommandService(UserPersistencePort userPersistencePort,
                                   SessionPersistencePort sessionPersistencePort,
                                   PasswordHasherPort passwordHasherPort,
                                   LoginRateLimiterPort loginRateLimiterPort,
                                   IdentityEventPublisherPort eventPublisherPort,
                                   int sessionDurationDays) {
        this.userPersistencePort = userPersistencePort;
        this.sessionPersistencePort = sessionPersistencePort;
        this.passwordHasherPort = passwordHasherPort;
        this.loginRateLimiterPort = loginRateLimiterPort;
        this.eventPublisherPort = eventPublisherPort;
        this.sessionDurationDays = sessionDurationDays;
    }

    @Override
    public UserId registerUser(RegisterUserCommand command) {
        if (userPersistencePort.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }
        if (userPersistencePort.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }

        String hashedPassword = passwordHasherPort.hash(command.rawPassword());
        User user = User.create(command.username(), hashedPassword, command.email(),
                command.firstName(), command.lastName());
        User saved = userPersistencePort.save(user);

        eventPublisherPort.publish(new UserRegistered(saved.getId(), saved.getEmail()));

        return saved.getId();
    }

    @Override
    public LoginResult authenticate(AuthenticateUserCommand command) {
        if (loginRateLimiterPort.isBlocked(command.clientIp())) {
            throw new RateLimitExceededException(command.clientIp());
        }

        User user = userPersistencePort.findByUsername(command.username())
                .filter(User::isActive)
                .orElse(null);

        if (user == null || !passwordHasherPort.matches(command.rawPassword(), user.getPasswordHash())) {
            loginRateLimiterPort.recordFailedAttempt(command.clientIp());
            throw new InvalidCredentialsException();
        }

        loginRateLimiterPort.resetAttempts(command.clientIp());

        Session session = Session.create(user.getId(), sessionDurationDays);
        Session saved = sessionPersistencePort.save(session);

        return new LoginResult(saved.getToken(), saved.getExpiresAt());
    }

    @Override
    public void logout(String token) {
        sessionPersistencePort.deleteByToken(token);
    }
}
