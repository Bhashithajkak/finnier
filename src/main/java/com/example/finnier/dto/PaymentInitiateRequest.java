package com.example.finnier.dto;

import com.example.finnier.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentInitiateRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String cardToken,

        String idempotencyKey
) {
}
