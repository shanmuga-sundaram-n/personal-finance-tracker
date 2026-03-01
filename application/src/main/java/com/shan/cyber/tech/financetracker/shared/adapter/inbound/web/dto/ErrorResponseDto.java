package com.shan.cyber.tech.financetracker.shared.adapter.inbound.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponseDto(
        int status,
        String error,
        String message,
        List<FieldErrorDto> errors,
        OffsetDateTime timestamp,
        String path) {

    public static ErrorResponseDto of(int status, String error, String message, String path) {
        return new ErrorResponseDto(status, error, message, List.of(), OffsetDateTime.now(), path);
    }

    public static ErrorResponseDto of(int status, String error, String message,
                                       List<FieldErrorDto> errors, String path) {
        return new ErrorResponseDto(status, error, message, errors, OffsetDateTime.now(), path);
    }
}
