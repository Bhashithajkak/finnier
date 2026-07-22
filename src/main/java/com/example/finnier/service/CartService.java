package com.example.finnier.service;

import com.example.finnier.entity.Cart;
import com.example.finnier.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public Cart createCart(Long customerId) {

        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomerId(customerId);
                    return cartRepository.save(cart);
                });
    }

    public Cart getCartByCustomerId(Long customerId) {

        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found for customer id : " + customerId));
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    public void deleteCart(Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        cartRepository.delete(cart);
    }
}