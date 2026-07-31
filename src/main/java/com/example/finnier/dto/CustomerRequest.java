package com.example.finnier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        String phoneNumber,
        String address)
{
}
