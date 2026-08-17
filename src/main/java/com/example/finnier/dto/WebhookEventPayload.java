package com.example.finnier.dto;

import com.example.finnier.enums.PaymentStatus;

import java.math.BigDecimal;

public record WebhookEventPayload(
        String eventType,
        String gatewayReference,
        String transactionId,
        PaymentStatus status,
        BigDecimal amount,
        String failureReason,
        Long timestamp
) {
}
