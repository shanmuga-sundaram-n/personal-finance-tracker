package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must contain only letters, numbers, and underscores")
        String username,

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName) {
}
