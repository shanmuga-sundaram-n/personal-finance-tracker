package com.shan.cyber.tech.financetracker.identity.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.util.Objects;

public class User {

    private UserId id;
    private String username;
    private String passwordHash;
    private String email;
    private String firstName;
    private String lastName;
    private boolean isActive;
    private String preferredCurrency;
    private AuditInfo auditInfo;

    public User(UserId id, String username, String passwordHash, String email,
                String firstName, String lastName, boolean isActive,
                String preferredCurrency, AuditInfo auditInfo) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
        this.isActive = isActive;
        this.preferredCurrency = preferredCurrency != null ? preferredCurrency : "USD";
        this.auditInfo = auditInfo;
    }

    public static User create(String username, String passwordHash, String email,
                               String firstName, String lastName) {
        return new User(null, username, passwordHash, email, firstName, lastName,
                true, "USD", null);
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateProfile(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
    }

    public void updateProfile(String firstName, String lastName, String preferredCurrency) {
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
        if (preferredCurrency == null || preferredCurrency.length() != 3)
            throw new IllegalArgumentException("preferredCurrency must be a 3-letter ISO 4217 code");
        this.preferredCurrency = preferredCurrency;
    }

    public void updatePreferredCurrency(String currency) {
        this.preferredCurrency = Objects.requireNonNull(currency, "currency must not be null");
    }

    public UserId getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isActive() { return isActive; }
    public String getPreferredCurrency() { return preferredCurrency; }
    public AuditInfo getAuditInfo() { return auditInfo; }

    public void setId(UserId id) { this.id = id; }
    public void setAuditInfo(AuditInfo auditInfo) { this.auditInfo = auditInfo; }
}
