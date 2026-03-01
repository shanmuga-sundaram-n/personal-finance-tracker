package com.shan.cyber.tech.financetracker.identity.domain.service;

import com.shan.cyber.tech.financetracker.identity.domain.exception.DuplicateEmailException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.DuplicateUsernameException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.InvalidCredentialsException;
import com.shan.cyber.tech.financetracker.identity.domain.exception.RateLimitExceededException;
import com.shan.cyber.tech.financetracker.identity.domain.model.Session;
import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.AuthenticateUserCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LoginResult;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.RegisterUserCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.IdentityEventPublisherPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.LoginRateLimiterPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.PasswordHasherPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.SessionPersistencePort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.UserPersistencePort;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityCommandServiceTest {

    @Mock private UserPersistencePort userPersistencePort;
    @Mock private SessionPersistencePort sessionPersistencePort;
    @Mock private PasswordHasherPort passwordHasherPort;
    @Mock private LoginRateLimiterPort loginRateLimiterPort;
    @Mock private IdentityEventPublisherPort eventPublisherPort;

    private IdentityCommandService service;

    @BeforeEach
    void setUp() {
        service = new IdentityCommandService(
                userPersistencePort, sessionPersistencePort,
                passwordHasherPort, loginRateLimiterPort,
                eventPublisherPort, 7);
    }

    @Test
    void registerUser_success() {
        RegisterUserCommand command = new RegisterUserCommand(
                "johndoe", "john@example.com", "password123", "John", "Doe");
        when(userPersistencePort.existsByUsername("johndoe")).thenReturn(false);
        when(userPersistencePort.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordHasherPort.hash("password123")).thenReturn("hashed");
        when(userPersistencePort.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(new UserId(1L));
            return u;
        });

        UserId result = service.registerUser(command);

        assertNotNull(result);
        assertEquals(1L, result.value());
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void registerUser_duplicateUsername_throws() {
        RegisterUserCommand command = new RegisterUserCommand(
                "johndoe", "john@example.com", "password123", "John", "Doe");
        when(userPersistencePort.existsByUsername("johndoe")).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> service.registerUser(command));
        verify(userPersistencePort, never()).save(any());
    }

    @Test
    void registerUser_duplicateEmail_throws() {
        RegisterUserCommand command = new RegisterUserCommand(
                "johndoe", "john@example.com", "password123", "John", "Doe");
        when(userPersistencePort.existsByUsername("johndoe")).thenReturn(false);
        when(userPersistencePort.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.registerUser(command));
        verify(userPersistencePort, never()).save(any());
    }

    @Test
    void authenticate_success() {
        AuthenticateUserCommand command = new AuthenticateUserCommand("johndoe", "password123", "127.0.0.1");
        User user = User.create("johndoe", "hashed", "john@example.com", "John", "Doe");
        user.setId(new UserId(1L));

        when(loginRateLimiterPort.isBlocked("127.0.0.1")).thenReturn(false);
        when(userPersistencePort.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordHasherPort.matches("password123", "hashed")).thenReturn(true);
        when(sessionPersistencePort.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        LoginResult result = service.authenticate(command);

        assertNotNull(result);
        assertNotNull(result.token());
        assertNotNull(result.expiresAt());
        verify(loginRateLimiterPort).resetAttempts("127.0.0.1");
    }

    @Test
    void authenticate_invalidPassword_throws() {
        AuthenticateUserCommand command = new AuthenticateUserCommand("johndoe", "wrong", "127.0.0.1");
        User user = User.create("johndoe", "hashed", "john@example.com", "John", "Doe");
        user.setId(new UserId(1L));

        when(loginRateLimiterPort.isBlocked("127.0.0.1")).thenReturn(false);
        when(userPersistencePort.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordHasherPort.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(command));
        verify(loginRateLimiterPort).recordFailedAttempt("127.0.0.1");
    }

    @Test
    void authenticate_userNotFound_throws() {
        AuthenticateUserCommand command = new AuthenticateUserCommand("unknown", "password", "127.0.0.1");

        when(loginRateLimiterPort.isBlocked("127.0.0.1")).thenReturn(false);
        when(userPersistencePort.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(command));
        verify(loginRateLimiterPort).recordFailedAttempt("127.0.0.1");
    }

    @Test
    void authenticate_rateLimited_throws() {
        AuthenticateUserCommand command = new AuthenticateUserCommand("johndoe", "password", "127.0.0.1");
        when(loginRateLimiterPort.isBlocked("127.0.0.1")).thenReturn(true);

        assertThrows(RateLimitExceededException.class, () -> service.authenticate(command));
        verify(userPersistencePort, never()).findByUsername(anyString());
    }

    @Test
    void authenticate_inactiveUser_throws() {
        AuthenticateUserCommand command = new AuthenticateUserCommand("johndoe", "password", "127.0.0.1");
        User user = User.create("johndoe", "hashed", "john@example.com", "John", "Doe");
        user.setId(new UserId(1L));
        user.deactivate();

        when(loginRateLimiterPort.isBlocked("127.0.0.1")).thenReturn(false);
        when(userPersistencePort.findByUsername("johndoe")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(command));
        verify(loginRateLimiterPort).recordFailedAttempt("127.0.0.1");
    }

    @Test
    void logout_deletesSession() {
        service.logout("some-token");
        verify(sessionPersistencePort).deleteByToken("some-token");
    }
}
