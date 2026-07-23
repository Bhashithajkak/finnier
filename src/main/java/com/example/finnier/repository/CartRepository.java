package com.example.finnier.repository;

import com.example.finnier.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerCustomerId(Long customerId);
}
