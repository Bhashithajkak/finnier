package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundRequest(
        @NotNull(message = "Payment ID is required")
        Long paymentId,

        BigDecimal amount,

        @NotBlank(message = "Refund reason is required")
        String reason
) {
}
