package com.shan.cyber.tech.financetracker.identity.adapter.outbound.security;

import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.LoginRateLimiterPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiterAdapter implements LoginRateLimiterPort {

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    public InMemoryRateLimiterAdapter(
            @Value("${app.auth.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.auth.rate-limit.window-minutes:5}") int windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    @Override
    public boolean isBlocked(String clientIp) {
        AttemptRecord record = attempts.get(clientIp);
        if (record == null) return false;
        if (isExpired(record)) {
            attempts.remove(clientIp);
            return false;
        }
        return record.count >= maxAttempts;
    }

    @Override
    public void recordFailedAttempt(String clientIp) {
        attempts.compute(clientIp, (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new AttemptRecord(1, Instant.now().toEpochMilli());
            }
            return new AttemptRecord(existing.count + 1, existing.windowStart);
        });
    }

    @Override
    public void resetAttempts(String clientIp) {
        attempts.remove(clientIp);
    }

    private boolean isExpired(AttemptRecord record) {
        return Instant.now().toEpochMilli() - record.windowStart > windowMillis;
    }

    private record AttemptRecord(int count, long windowStart) {}
}
