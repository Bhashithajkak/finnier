package com.example.finnier.repository;

import com.example.finnier.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGatewayReference(String gatewayReference);

    Optional<Payment> findByOrderOrderId(Long orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);


}
