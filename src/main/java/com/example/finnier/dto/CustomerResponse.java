package com.example.finnier.dto;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long customerId,
        String email,
        String phoneNumber,
        String address,
        int loyaltyPoints,
        LocalDateTime dateJoined,
        LocalDateTime lastUpdated

) {
}
