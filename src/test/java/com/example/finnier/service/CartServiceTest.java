package com.example.finnier.service;

import com.example.finnier.entity.Cart;
import com.example.finnier.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId(1L);
        cart.setCustomerId(100L);
        cart.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createCart_shouldCreateNewCart_whenCustomerHasNoCart() {

        when(cartRepository.findByCustomerId(100L))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class)))
                .thenReturn(cart);

        Cart result = cartService.createCart(100L);

        assertNotNull(result);
        assertEquals(100L, result.getCustomerId());

        verify(cartRepository).findByCustomerId(100L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createCart_shouldReturnExistingCart_whenCustomerAlreadyHasCart() {

        when(cartRepository.findByCustomerId(100L))
                .thenReturn(Optional.of(cart));

        Cart result = cartService.createCart(100L);

        assertNotNull(result);
        assertEquals(1L, result.getCartId());

        verify(cartRepository).findByCustomerId(100L);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getCartByCustomerId_shouldReturnCart() {

        when(cartRepository.findByCustomerId(100L))
                .thenReturn(Optional.of(cart));

        Cart result = cartService.getCartByCustomerId(100L);

        assertNotNull(result);
        assertEquals(100L, result.getCustomerId());

        verify(cartRepository).findByCustomerId(100L);
    }

    @Test
    void getCartByCustomerId_shouldThrowException_whenCartNotFound() {

        when(cartRepository.findByCustomerId(100L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cartService.getCartByCustomerId(100L)
        );

        assertEquals("Cart not found for customer id : 100", exception.getMessage());

        verify(cartRepository).findByCustomerId(100L);
    }

    @Test
    void getAllCarts_shouldReturnAllCarts() {

        Cart cart2 = new Cart();
        cart2.setCartId(2L);
        cart2.setCustomerId(200L);

        when(cartRepository.findAll())
                .thenReturn(List.of(cart, cart2));

        List<Cart> carts = cartService.getAllCarts();

        assertEquals(2, carts.size());

        verify(cartRepository).findAll();
    }

    @Test
    void deleteCart_shouldDeleteCart() {

        when(cartRepository.findById(1L))
                .thenReturn(Optional.of(cart));

        cartService.deleteCart(1L);

        verify(cartRepository).findById(1L);
        verify(cartRepository).delete(cart);
    }

    @Test
    void deleteCart_shouldThrowException_whenCartNotFound() {

        when(cartRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cartService.deleteCart(1L)
        );

        assertEquals("Cart not found", exception.getMessage());

        verify(cartRepository).findById(1L);
        verify(cartRepository, never()).delete(any(Cart.class));
    }
}