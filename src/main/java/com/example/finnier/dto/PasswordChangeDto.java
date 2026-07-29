package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordChangeDto(
        @NotBlank
        String currentPassword,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,64}$",
                message = "Password must be 8-64 characters and include uppercase, lowercase, a digit, and a special character"
        )
        String newPassword,
        @NotBlank
        String confirmNewPassword
) {}