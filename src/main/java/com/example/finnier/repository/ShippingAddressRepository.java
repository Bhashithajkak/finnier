package com.example.finnier.repository;

import com.example.finnier.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findByCustomerCustomerId(Long customerId);
}
