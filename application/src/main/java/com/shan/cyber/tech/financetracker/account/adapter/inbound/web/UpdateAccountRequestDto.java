package com.shan.cyber.tech.financetracker.account.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequestDto(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String institutionName) {
}
