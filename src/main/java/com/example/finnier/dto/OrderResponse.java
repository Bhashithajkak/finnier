package com.example.finnier.dto;

import com.example.finnier.entity.Order;
import com.example.finnier.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long customerId,
        String customerEmail,
        ShippingAddressDto shippingAddress,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        Order.OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items
) {
}
