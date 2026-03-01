package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.identity.domain.model.Session;
import com.shan.cyber.tech.financetracker.identity.domain.port.outbound.SessionPersistencePort;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

public class SessionAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login"
    );

    private final SessionPersistencePort sessionPersistencePort;

    public SessionAuthFilter(SessionPersistencePort sessionPersistencePort) {
        this.sessionPersistencePort = sessionPersistencePort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();

            if (isPublicPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = extractToken(request);
            if (token == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid Authorization header\"}");
                return;
            }

            Session session = sessionPersistencePort.findValidSession(token, OffsetDateTime.now()).orElse(null);
            if (session == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or expired session token\"}");
                return;
            }

            SecurityContextHolder.setCurrentUserId(session.getUserId().value());
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
