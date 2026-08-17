package com.example.finnier.dto;

import com.example.finnier.enums.PaymentMethod;
import com.example.finnier.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        String transactionId,
        String gatewayReference,
        String failureReason,
        LocalDateTime paymentDate,
        LocalDateTime updatedAt
) {
}
