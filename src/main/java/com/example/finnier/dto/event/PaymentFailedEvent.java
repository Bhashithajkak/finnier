package com.example.finnier.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        String customerEmail,
        BigDecimal amount,
        String failureReason,
        LocalDateTime failedAt
) {
}
