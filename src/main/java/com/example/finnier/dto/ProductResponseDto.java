package com.example.finnier.dto;

import com.example.finnier.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponseDto(
        Long productId,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        int quantity,
        Product.Status status,
        LocalDateTime createdAt
) {}
