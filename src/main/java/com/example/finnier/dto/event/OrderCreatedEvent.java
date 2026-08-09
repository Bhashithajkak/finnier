package com.example.finnier.dto.event;

import com.example.finnier.entity.Order;
import com.example.finnier.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        String customerEmail,
        BigDecimal totalAmount,
        Order.OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        LocalDateTime orderDate,
        List<OrderItemEventInfo> items
) {
    public record OrderItemEventInfo(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
