package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveCardRequest(
        @NotBlank(message = "Card token is required")
        String cardToken
) {
}
