package com.example.finnier.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
