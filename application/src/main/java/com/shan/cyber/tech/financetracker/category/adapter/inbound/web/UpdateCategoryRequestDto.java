package com.shan.cyber.tech.financetracker.category.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequestDto(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) String icon,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g. #FF5733)") String color) {
}
