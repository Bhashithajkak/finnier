package com.example.finnier.controller;

import com.example.finnier.dto.CartItemRequest;
import com.example.finnier.dto.CartItemResponse;
import com.example.finnier.dto.CartResponse;
import com.example.finnier.entity.Cart;
import com.example.finnier.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService){this.cartService = cartService;}
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

    @PostMapping("/items/")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody CartItemRequest cartItemRequest){
        CartResponse cartResponse = cartService.addItemToCart(cartItemRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartResponse);
    }

    @GetMapping("/items/{cartId}")
    public ResponseEntity<List<CartItemResponse>> getCartItems(@PathVariable Long cartId){
        List<CartItemResponse> items = cartService.getCartItems(cartId);
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable Long itemId){
        cartService.removeCartItem(itemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items/")
    public ResponseEntity<CartResponse> updateCartItem(@Valid @RequestBody CartItemRequest cartItemRequest){
        return ResponseEntity.ok(cartService.updateCartItemQty(cartItemRequest));
    }
}