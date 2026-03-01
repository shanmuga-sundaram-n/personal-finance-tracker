package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.AuthenticateUserCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.AuthenticateUserUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.GetCurrentUserQuery;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LoginResult;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.LogoutUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.RegisterUserCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.RegisterUserUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserQuery getCurrentUserQuery;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                           AuthenticateUserUseCase authenticateUserUseCase,
                           LogoutUseCase logoutUseCase,
                           GetCurrentUserQuery getCurrentUserQuery) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserQuery = getCurrentUserQuery;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponseDto register(@Valid @RequestBody RegisterRequestDto dto) {
        UserId userId = registerUserUseCase.registerUser(new RegisterUserCommand(
                dto.username(), dto.email(), dto.password(), dto.firstName(), dto.lastName()));

        UserProfile profile = getCurrentUserQuery.getCurrentUser(userId);
        return toResponseDto(profile);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto dto,
                                   HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        LoginResult result = authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(dto.username(), dto.password(), clientIp));

        return new LoginResponseDto(result.token(), result.expiresAt());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            logoutUseCase.logout(token);
        }
    }

    @GetMapping("/me")
    public UserProfileResponseDto me() {
        Long userId = SecurityContextHolder.getCurrentUserId();
        UserProfile profile = getCurrentUserQuery.getCurrentUser(new UserId(userId));
        return toResponseDto(profile);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private UserProfileResponseDto toResponseDto(UserProfile profile) {
        return new UserProfileResponseDto(
                profile.id(), profile.username(), profile.email(),
                profile.firstName(), profile.lastName(),
                profile.preferredCurrency(), profile.createdAt());
    }
}
