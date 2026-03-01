package com.shan.cyber.tech.financetracker.identity.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank String username,
        @NotBlank String password) {
}
