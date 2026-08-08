package com.example.finnier.controller;

import com.example.finnier.dto.PasswordChangeDto;
import com.example.finnier.dto.UserRequest;
import com.example.finnier.dto.UserResponse;
import com.example.finnier.entity.User;
import com.example.finnier.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        UserResponse response = userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    @Operation(
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/email/{email}")
    @Operation(
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<List<UserResponse>> getAllUsersByRole(@PathVariable User.RoleType role) {
        return ResponseEntity.ok(userService.getAllUsersByRole(role));
    }

    @GetMapping("/status/{status}")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<List<UserResponse>> getAllUsersByStatus(@PathVariable User.UserStatus status) {
        return ResponseEntity.ok(userService.getAllUsersByStatus(status));
    }

    @PutMapping("/{userId}")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId,
                                                   @Valid @RequestBody UserRequest userRequest) {
        return ResponseEntity.ok(userService.updateUser(userId, userRequest));
    }

    @PatchMapping("/{userId}/status")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long userId,
                                                         @RequestParam User.UserStatus status) {
        return ResponseEntity.ok(userService.updateUserStatus(userId, status));
    }

    @PatchMapping("/{userId}/deactivate")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<Void> deactivateUserStatus(@PathVariable Long userId) {
        userService.deactivateUserStatus(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/role")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long userId,
                                                       @RequestParam User.RoleType role) {
        return ResponseEntity.ok(userService.updateUserRole(userId, role));
    }

    @PatchMapping("/{userId}/password")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<UserResponse> updateUserPassword(@PathVariable Long userId,
                                                           @Valid @RequestBody PasswordChangeDto passwordChangeDto) {
        return ResponseEntity.ok(userService.updateUserPassword(userId, passwordChangeDto));
    }

    @DeleteMapping("/{userId}")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}