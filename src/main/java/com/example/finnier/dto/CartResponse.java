package com.example.finnier.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long cartId,
        Long customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        LocalDateTime updatedAt
) {

}
