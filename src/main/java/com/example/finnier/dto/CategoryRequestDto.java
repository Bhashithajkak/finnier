package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequestDto(

        @NotBlank(message = "Category name is required")
        String categoryName

) {}