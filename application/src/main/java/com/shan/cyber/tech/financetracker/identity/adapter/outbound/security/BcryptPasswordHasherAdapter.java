package com.shan.cyber.tech.financetracker.identity.adapter.outbound.security;

import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasherAdapter implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
