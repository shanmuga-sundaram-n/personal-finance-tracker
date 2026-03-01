package com.shan.cyber.tech.financetracker.identity.domain.port.outbound;

public interface LoginRateLimiterPort {
    boolean isBlocked(String clientIp);
    void recordFailedAttempt(String clientIp);
    void resetAttempts(String clientIp);
}
