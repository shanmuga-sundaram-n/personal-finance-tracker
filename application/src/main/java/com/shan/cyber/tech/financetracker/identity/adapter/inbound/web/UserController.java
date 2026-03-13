package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileCommand;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UpdateUserProfileUseCase;
import com.shan.cyber.tech.financetracker.identity.domain.port.inbound.UserProfile;
import com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.SecurityContextHolder;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;

    public UserController(UpdateUserProfileUseCase updateUserProfileUseCase) {
        this.updateUserProfileUseCase = updateUserProfileUseCase;
    }

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
