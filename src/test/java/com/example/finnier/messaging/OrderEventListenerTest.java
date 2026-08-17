package com.example.finnier.messaging;

import com.example.finnier.dto.event.OrderCreatedEvent;
import com.example.finnier.dto.event.OrderStatusUpdateEvent;
import com.example.finnier.entity.Order;
import com.example.finnier.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Spy
    private OrderEventListener listener;

    // handleOrderCreatedEvent
    @Test
    void handleOrderCreatedEvent_shouldInvokedOnce_whenEventReceived() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        listener.handleOrderCreatedEvent(event);

        verify(listener, times(1)).handleOrderCreatedEvent(event);
    }

    @Test
    void handleOrderCreatedEvent_shouldNotThrowException() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        assertThatCode(() -> listener.handleOrderCreatedEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void handleOrderCreatedEvent_shouldNotThrowException_whenAcceptsMultipleItems() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                2000L, 20L, "bulk@example.com",
                new BigDecimal("1500.00"),
                Order.OrderStatus.PENDING,
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                List.of(
                        new OrderCreatedEvent.OrderItemEventInfo(101L, "Vase",   1, new BigDecimal("500.00"), new BigDecimal("500.00")),
                        new OrderCreatedEvent.OrderItemEventInfo(102L, "Canvas", 2, new BigDecimal("250.00"), new BigDecimal("500.00")),
                        new OrderCreatedEvent.OrderItemEventInfo(103L, "Brush",  5, new BigDecimal("100.00"), new BigDecimal("500.00"))
                )
        );

        org.assertj.core.api.Assertions.assertThatCode(() -> listener.handleOrderCreatedEvent(event))
                .doesNotThrowAnyException();
    }


    // handleOrderStatusUpdateEvent

    @Test
    void handleOrderStatusUpdateEvent_shouldInvokedOnce_whenEventReceived() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.SHIPPED);

        listener.handleOrderStatusUpdateEvent(event);

        verify(listener, times(1)).handleOrderStatusUpdateEvent(event);
    }

    @Test
    void handleOrderStatusUpdateEvent_shouldNotThrowException_whenBuildStatusUpdateEvent() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.PAID);

        assertThatCode(() -> listener.handleOrderStatusUpdateEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void handleOrderStatusUpdateEvent_handlesAllStatuses_cancelled() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.CANCELLED);
        assertThatCode(() -> listener.handleOrderStatusUpdateEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void handleOrderStatusUpdateEvent_handlesAllStatuses_delivered() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.DELIVERED);

        org.assertj.core.api.Assertions.assertThatCode(() -> listener.handleOrderStatusUpdateEvent(event))
                .doesNotThrowAnyException();
    }

    // Helpers

    private OrderCreatedEvent buildOrderCreatedEvent() {
        return new OrderCreatedEvent(
                1000L, 10L, "customer@example.com",
                new BigDecimal("500.00"),
                Order.OrderStatus.PENDING,
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                List.of(new OrderCreatedEvent.OrderItemEventInfo(
                        100L, "Handmade Vase", 2,
                        new BigDecimal("250.00"), new BigDecimal("500.00")))
        );
    }

    private OrderStatusUpdateEvent buildStatusUpdateEvent(Order.OrderStatus newStatus) {
        return new OrderStatusUpdateEvent(
                1000L, 10L, "customer@example.com",
                Order.OrderStatus.PENDING,
                newStatus,
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );
    }
}
