package com.example.finnier.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRefundedEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        String customerEmail,
        BigDecimal refundAmount,
        String reason,
        LocalDateTime refundedAt
) {
}
