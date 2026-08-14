package com.example.finnier.gateway;

import java.math.BigDecimal;

public record PaymentGatewayRequest(
        Long orderId,
        BigDecimal amount,
        String currency,
        String customerEmail,
        String cardToken,
        String description,
        String idempotencyKey
) {
}
