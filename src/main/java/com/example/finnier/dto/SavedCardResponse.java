package com.example.finnier.dto;

public record SavedCardResponse(
        String savedCardToken,
        String maskedCardNumber,
        String cardBrand
) {
}
