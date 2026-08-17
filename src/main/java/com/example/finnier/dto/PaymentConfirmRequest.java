package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
        @NotBlank(message = "Gateway reference is required")
        String gatewayReference
) {
}
