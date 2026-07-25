package com.example.finnier.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @Nullable
        Long cartItemId,
        @NotNull
        Long customerId,
        @NotNull
        Long productId,
        @Min(1)
        int quantity
) {
}
