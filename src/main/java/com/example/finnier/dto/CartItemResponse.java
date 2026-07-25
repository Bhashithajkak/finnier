package com.example.finnier.dto;

import java.math.BigDecimal;
public record CartItemResponse(
        Long cartItemId,
        Long productionId,
        String productName,
        int quantity,
        BigDecimal subtotal
) {
}
