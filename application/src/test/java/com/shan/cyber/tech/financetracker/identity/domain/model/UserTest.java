package com.shan.cyber.tech.financetracker.identity.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void create_setsDefaultCurrencyToUSD() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        assertEquals("USD", user.getPreferredCurrency());
        assertTrue(user.isActive());
        assertNull(user.getId());
    }

    @Test
    void create_setsProvidedFields() {
        User user = User.create("alice", "hash", "alice@example.com", "Alice", "Smith");
        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("Alice", user.getFirstName());
        assertEquals("Smith", user.getLastName());
    }

    @Test
    void deactivate_setsInactiveFlag() {
        User user = User.create("bob", "hash", "bob@example.com", "Bob", "Brown");
        user.deactivate();
        assertFalse(user.isActive());
    }

    @Test
    void activate_restoresActiveFlag() {
        User user = User.create("bob", "hash", "bob@example.com", "Bob", "Brown");
        user.deactivate();
        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    void updateProfile_twoArgs_updatesNamesOnly() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        user.updateProfile("Jane", "Smith");
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("USD", user.getPreferredCurrency()); // unchanged
    }

    @Test
    void updateProfile_threeArgs_updatesNamesAndCurrency() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        user.updateProfile("John", "Doe", "EUR");
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("EUR", user.getPreferredCurrency());
    }

    @Test
    void updateProfile_threeArgs_invalidCurrencyLength_throwsIllegalArgument() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        assertThrows(IllegalArgumentException.class,
                () -> user.updateProfile("John", "Doe", "US")); // only 2 chars
    }

    @Test
    void updateProfile_threeArgs_nullCurrency_throwsIllegalArgument() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        assertThrows(IllegalArgumentException.class,
                () -> user.updateProfile("John", "Doe", null));
    }

    @Test
    void updateProfile_twoArgs_nullFirstName_throwsNullPointer() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        assertThrows(NullPointerException.class,
                () -> user.updateProfile(null, "Doe"));
    }

    @Test
    void updatePreferredCurrency_updatesCurrency() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        user.updatePreferredCurrency("GBP");
        assertEquals("GBP", user.getPreferredCurrency());
    }

    @Test
    void setId_setsUserId() {
        User user = User.create("john", "hash", "john@example.com", "John", "Doe");
        user.setId(new UserId(42L));
        assertEquals(42L, user.getId().value());
    }
}
