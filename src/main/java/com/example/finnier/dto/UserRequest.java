package com.example.finnier.dto;

import com.example.finnier.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @Email
        @NotBlank
        String email,
        @NotBlank
        @Size(min = 8)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
                message = "Password must be at least 8 characters and include uppercase, lowercase, a digit, and a special character"
        )
        String password,

        User.RoleType role,
        User.UserStatus status
) {
}
