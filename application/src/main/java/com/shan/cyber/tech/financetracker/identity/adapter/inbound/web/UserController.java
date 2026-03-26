package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileUseCase;
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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Manage the authenticated user's profile settings")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;

    public UserController(UpdateUserProfileUseCase updateUserProfileUseCase) {
        this.updateUserProfileUseCase = updateUserProfileUseCase;
    }

    @Operation(
        summary = "Update the authenticated user's profile",
        description = "Updates first name, last name, and preferred currency for the currently logged-in user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — invalid field values",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto"))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponseDto")))
    })
    @PutMapping("/me")
    public UserProfileResponseDto updateMe(@Valid @RequestBody UpdateProfileRequestDto dto) {
        Long userId = SecurityContextHolder.getCurrentUserId();
        UserProfile profile = updateUserProfileUseCase.updateProfile(
                new UpdateUserProfileCommand(new UserId(userId), dto.firstName(), dto.lastName(), dto.preferredCurrency()));

        return new UserProfileResponseDto(
                profile.id(), profile.username(), profile.email(),
                profile.firstName(), profile.lastName(),
                profile.preferredCurrency(), profile.createdAt());
    }
}
