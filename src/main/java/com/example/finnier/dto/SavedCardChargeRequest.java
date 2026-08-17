package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavedCardChargeRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotBlank(message = "Saved card token is required")
        String savedCardToken,

        String idempotencyKey
) {
}
