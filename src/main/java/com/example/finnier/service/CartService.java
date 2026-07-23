package com.example.finnier.service;

import com.example.finnier.entity.Cart;
import com.example.finnier.entity.Customer;
import com.example.finnier.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CustomerService customerService;

    public CartService(CartRepository cartRepository, CustomerService customerService) {
        this.cartRepository = cartRepository;
        this.customerService = customerService;
    }

    public Cart createCart(Long customerId) {

        Customer customer = customerService.findCustomerById(customerId);

        return cartRepository.findByCustomerCustomerId(customerId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }

    public Cart getCartByCustomerId(Long customerId) {

        return cartRepository.findByCustomerCustomerId(customerId)
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