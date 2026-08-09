package com.example.finnier.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressDto(
        @NotBlank(message = "Recipient name is required")
        String recipientName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Address line 1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "District is required")
        String district,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "Country is required")
        String country
) {
}
