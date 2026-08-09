package com.example.finnier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        List<Long> cartItemIds,

        Long shippingAddressId,

        @Valid
        ShippingAddressDto newShippingAddress
) {
    public boolean hasValidShippingAddress() {
        return shippingAddressId != null || newShippingAddress != null;
    }
}
