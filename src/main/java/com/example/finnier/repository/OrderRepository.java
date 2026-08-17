package com.example.finnier.repository;

import com.example.finnier.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerCustomerId(Long customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.user.email = :email")
    List<Order> findByCustomerUserEmail(@Param("email") String email);

    @Query("SELECT o FROM Order o WHERE o.customer.user.email = :email")
    Page<Order> findByCustomerUserEmail(@Param("email") String email, Pageable pageable);

    List<Order> findByOrderStatus(Order.OrderStatus orderStatus);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer c JOIN FETCH c.user u WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}
