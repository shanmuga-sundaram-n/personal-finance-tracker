package com.shan.cyber.tech.financetracker.identity.config;

import com.shan.cyber.tech.financetracker.identity.adapter.inbound.web.SessionAuthFilter;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.IdentityEventPublisherPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.LoginRateLimiterPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.PasswordHasherPort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.SessionPersistencePort;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.UserPersistencePort;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.service.IdentityCommandService;
import com.shan.cyber.tech.financetracker.identity.domain.service.IdentityQueryService;
import com.shan.cyber.tech.financetracker.identity.domain.service.UpdateUserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityConfig {

    @Value("${app.session.duration-days}")
    private int sessionDurationDays;

    @Bean
    public IdentityCommandService identityCommandService(
            UserPersistencePort userPersistencePort,
            SessionPersistencePort sessionPersistencePort,
            PasswordHasherPort passwordHasherPort,
            LoginRateLimiterPort loginRateLimiterPort,
            IdentityEventPublisherPort identityEventPublisherPort) {
        return new IdentityCommandService(userPersistencePort, sessionPersistencePort,
                passwordHasherPort, loginRateLimiterPort, identityEventPublisherPort, sessionDurationDays);
    }

    @Bean
    public IdentityQueryService identityQueryService(UserPersistencePort userPersistencePort) {
        return new IdentityQueryService(userPersistencePort);
    }

    @Bean
    public UpdateUserProfileUseCase updateUserProfileUseCase(UserPersistencePort userPersistencePort) {
        return new UpdateUserProfileService(userPersistencePort);
    }

    @Bean
    public FilterRegistrationBean<SessionAuthFilter> sessionAuthFilterRegistration(
            SessionPersistencePort sessionPersistencePort) {
        FilterRegistrationBean<SessionAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SessionAuthFilter(sessionPersistencePort));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
