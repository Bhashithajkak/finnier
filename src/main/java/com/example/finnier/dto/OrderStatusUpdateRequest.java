package com.example.finnier.dto;

import com.example.finnier.entity.Order;
import com.example.finnier.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Order status is required")
        Order.OrderStatus orderStatus,

        PaymentStatus paymentStatus
) {
}
