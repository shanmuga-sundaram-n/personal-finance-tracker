package com.shan.cyber.tech.financetracker.identity.domain.port.outbound;

public interface PasswordHasherPort {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
