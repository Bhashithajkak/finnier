package com.example.finnier.dto;

import com.example.finnier.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String firstName,
        String lastName,
        String email,
        User.RoleType role,
        User.UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
)  {
}
