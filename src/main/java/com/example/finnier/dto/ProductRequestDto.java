package com.example.finnier.dto;

import com.example.finnier.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequestDto(
        @NotNull
        Long categoryId,

        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        @Positive
        BigDecimal price,

        @PositiveOrZero
        int quantity,

        @NotNull
        Product.Status status
) {
}