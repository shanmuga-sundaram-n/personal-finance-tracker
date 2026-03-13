package com.shan.cyber.tech.financetracker.identity.domain.service;

import com.shan.cyber.tech.financetracker.identity.domain.exception.UserNotFoundException;
import com.shan.cyber.tech.financetracker.identity.domain.model.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityQueryServiceTest {

    @Mock private UserPersistencePort userPersistencePort;

    private IdentityQueryService service;

    @BeforeEach
    void setUp() {
        service = new IdentityQueryService(userPersistencePort);
    }

    @Test
    void getCurrentUser_found_returnsProfile() {
        UserId userId = new UserId(1L);
        User user = User.create("johndoe", "hash", "john@example.com", "John", "Doe");
        user.setId(userId);
        when(userPersistencePort.findById(userId)).thenReturn(Optional.of(user));

        UserProfile profile = service.getCurrentUser(userId);

        assertNotNull(profile);
        assertEquals(1L, profile.id());
        assertEquals("johndoe", profile.username());
        assertEquals("john@example.com", profile.email());
        assertEquals("John", profile.firstName());
        assertEquals("Doe", profile.lastName());
        assertEquals("USD", profile.preferredCurrency());
    }

    @Test
    void getCurrentUser_notFound_throwsUserNotFoundException() {
        UserId userId = new UserId(999L);
        when(userPersistencePort.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getCurrentUser(userId));
    }
}
