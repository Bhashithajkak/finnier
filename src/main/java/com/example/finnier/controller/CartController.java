package com.example.finnier.controller;

import com.example.finnier.entity.Cart;
import com.example.finnier.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/{customerId}")
    public ResponseEntity<Cart> createCart(@PathVariable Long customerId) {

        Cart cart = cartService.createCart(customerId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cart);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Cart> getCartByCustomerId(@PathVariable Long customerId) {

        return ResponseEntity.ok(
                cartService.getCartByCustomerId(customerId)
        );
    }

    @GetMapping
    public ResponseEntity<List<Cart>> getAllCarts() {

        return ResponseEntity.ok(
                cartService.getAllCarts()
        );
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long cartId) {

        cartService.deleteCart(cartId);

        return ResponseEntity.noContent().build();
    }
}