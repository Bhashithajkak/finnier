package com.example.finnier.dto;

import jakarta.validation.constraints.Email;

public record LoginRequest(
        @Email
        String email,
        String password
) {
}
