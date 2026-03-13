package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "Must be 3-letter ISO 4217 code")
        String preferredCurrency) {
}
