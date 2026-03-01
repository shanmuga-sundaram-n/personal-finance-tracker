package com.shan.cyber.tech.financetracker.identity.domain.exception;

import com.shan.cyber.tech.financetracker.shared.domain.exception.DomainException;

public class RateLimitExceededException extends DomainException {
    public RateLimitExceededException(String clientIp) {
        super("RATE_LIMIT_EXCEEDED", "Too many login attempts from " + clientIp + ". Please try again later.");
    }
}
