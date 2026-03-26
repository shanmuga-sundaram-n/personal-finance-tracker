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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Authentication", description = "User registration, login, logout, and session operations")
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

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Username and email must be unique. Returns the created user profile."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "422", description = "Business rule violation — username or email already taken",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponseDto register(@Valid @RequestBody RegisterRequestDto dto) {
        UserId userId = registerUserUseCase.registerUser(new RegisterUserCommand(
                dto.username(), dto.email(), dto.password(), dto.firstName(), dto.lastName()));

        UserProfile profile = getCurrentUserQuery.getCurrentUser(userId);
        return toResponseDto(profile);
    }

    @Operation(
        summary = "Authenticate a user",
        description = "Validates credentials and returns an opaque session token (UUID). Pass the token as 'Authorization: Bearer {token}' on subsequent requests."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — returns session token",
            content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing username or password",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto dto,
                                   HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        LoginResult result = authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(dto.username(), dto.password(), clientIp));

        return new LoginResponseDto(result.token(), result.expiresAt());
    }

    @Operation(
        summary = "Invalidate the current session",
        description = "Revokes the bearer token supplied in the Authorization header. No-op if the token is already expired or absent.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session invalidated — no content returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            logoutUseCase.logout(token);
        }
    }

    @Operation(
        summary = "Get the authenticated user's profile",
        description = "Returns the profile of the user identified by the current session token.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns the authenticated user's profile",
            content = @Content(schema = @Schema(implementation = UserProfileResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
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
