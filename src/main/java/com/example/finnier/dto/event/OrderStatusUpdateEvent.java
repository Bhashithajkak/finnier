package com.example.finnier.dto.event;

import com.example.finnier.entity.Order;
import com.example.finnier.enums.PaymentStatus;

import java.time.LocalDateTime;

public record OrderStatusUpdateEvent(
        Long orderId,
        Long customerId,
        String customerEmail,
        Order.OrderStatus previousStatus,
        Order.OrderStatus newStatus,
        PaymentStatus paymentStatus,
        LocalDateTime updatedAt
) {
}
