package com.shan.cyber.tech.financetracker.identity.domain.service;

import com.shan.cyber.tech.financetracker.identity.domain.exception.UserNotFoundException;
import com.shan.cyber.tech.financetracker.identity.domain.model.User;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserProfileServiceTest {

    @Mock private UserPersistencePort userPersistencePort;

    private UpdateUserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UpdateUserProfileService(userPersistencePort);
    }

    @Test
    void updateProfile_success_returnsUpdatedProfile() {
        UserId userId = new UserId(1L);
        User user = User.create("johndoe", "hash", "john@example.com", "John", "Doe");
        user.setId(userId);

        when(userPersistencePort.findById(userId)).thenReturn(Optional.of(user));
        when(userPersistencePort.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileCommand command = new UpdateUserProfileCommand(userId, "Jane", "Smith", "EUR");
        UserProfile result = service.updateProfile(command);

        assertNotNull(result);
        assertEquals("Jane", result.firstName());
        assertEquals("Smith", result.lastName());
        assertEquals("EUR", result.preferredCurrency());
        verify(userPersistencePort).save(user);
    }

    @Test
    void updateProfile_userNotFound_throwsUserNotFoundException() {
        UserId userId = new UserId(999L);
        when(userPersistencePort.findById(userId)).thenReturn(Optional.empty());

        UpdateUserProfileCommand command = new UpdateUserProfileCommand(userId, "Jane", "Smith", "EUR");

        assertThrows(UserNotFoundException.class, () -> service.updateProfile(command));
        verify(userPersistencePort, never()).save(any());
    }

    @Test
    void updateProfile_invalidCurrency_throwsIllegalArgument() {
        UserId userId = new UserId(1L);
        User user = User.create("johndoe", "hash", "john@example.com", "John", "Doe");
        user.setId(userId);
        when(userPersistencePort.findById(userId)).thenReturn(Optional.of(user));

        UpdateUserProfileCommand command = new UpdateUserProfileCommand(userId, "Jane", "Smith", "XX");

        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(command));
        verify(userPersistencePort, never()).save(any());
    }
}
