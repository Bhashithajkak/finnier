package com.example.finnier.controller;

import com.example.finnier.dto.CartItemRequest;
import com.example.finnier.dto.CartItemResponse;
import com.example.finnier.dto.CartResponse;
import com.example.finnier.entity.Cart;
import com.example.finnier.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/{customerId}")
    @Operation(summary = "Create a cart for a customer")
    public ResponseEntity<Cart> createCart(@PathVariable Long customerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.createCart(customerId));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get cart by customer ID")
    public ResponseEntity<Cart> getCartByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(cartService.getCartByCustomerId(customerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all carts (admin only)")
    public ResponseEntity<List<Cart>> getAllCarts() {
        return ResponseEntity.ok(cartService.getAllCarts());
    }

    @DeleteMapping("/{cartId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete a cart (admin/staff only)")
    public ResponseEntity<Void> deleteCart(@PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items/")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody CartItemRequest cartItemRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItemToCart(cartItemRequest));
    }

    @GetMapping("/items/{cartId}")
    @Operation(summary = "Get items in a cart")
    public ResponseEntity<List<CartItemResponse>> getCartItems(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartService.getCartItems(cartId));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a cart item")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long itemId) {
        cartService.removeCartItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/items/")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartResponse> updateCartItem(@Valid @RequestBody CartItemRequest cartItemRequest) {
        return ResponseEntity.ok(cartService.updateCartItemQty(cartItemRequest));
    }
}