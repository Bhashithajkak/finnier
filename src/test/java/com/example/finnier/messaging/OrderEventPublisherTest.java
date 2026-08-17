package com.example.finnier.messaging;

import com.example.finnier.dto.event.OrderCreatedEvent;
import com.example.finnier.dto.event.OrderStatusUpdateEvent;
import com.example.finnier.entity.Order;
import com.example.finnier.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderEventPublisher publisher;

    private static final String EXCHANGE      = "order.exchange";
    private static final String CREATED_KEY   = "order.created";
    private static final String STATUS_KEY    = "order.status";

    @BeforeEach
    void setUp() {
        publisher = new OrderEventPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchangeName",            EXCHANGE);
        ReflectionTestUtils.setField(publisher, "orderCreatedRoutingKey",  CREATED_KEY);
        ReflectionTestUtils.setField(publisher, "orderStatusRoutingKey",   STATUS_KEY);
    }


    @Test
    void publishOrderCreated_sendsToCorrectDestination() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        publisher.publishOrderCreated(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, CREATED_KEY, event);
    }

    @Test
    void publishOrderCreated_passesCorrectPayload() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        publisher.publishOrderCreated(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(CREATED_KEY), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishOrderCreated_doesNotPropagate_onRabbitException() {
        doThrow(new RuntimeException("RabbitMQ connection lost"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        OrderCreatedEvent event = buildOrderCreatedEvent();

        // must not throw
        publisher.publishOrderCreated(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, CREATED_KEY, event);
    }


    @Test
    void publishOrderStatusUpdated_sendsWithCorrectRoutingKey() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.SHIPPED);

        publisher.publishOrderStatusUpdated(event);

        String expectedKey = STATUS_KEY + ".shipped";
        verify(rabbitTemplate).convertAndSend(EXCHANGE, expectedKey, event);
    }

    @Test
    void publishOrderStatusUpdated_routingKey_cancelled() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.CANCELLED);

        publisher.publishOrderStatusUpdated(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, STATUS_KEY + ".cancelled", event);
    }

    @Test
    void publishOrderStatusUpdated_routingKey_paid() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.PAID);

        publisher.publishOrderStatusUpdated(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, STATUS_KEY + ".paid", event);
    }

    @Test
    void publishOrderStatusUpdated_routingKey_delivered() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.DELIVERED);

        publisher.publishOrderStatusUpdated(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, STATUS_KEY + ".delivered", event);
    }

    @Test
    void publishOrderStatusUpdated_passesCorrectPayload() {
        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.PAID);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        publisher.publishOrderStatusUpdated(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishOrderStatusUpdated_doesNotPropagate_onRabbitException() {
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        OrderStatusUpdateEvent event = buildStatusUpdateEvent(Order.OrderStatus.SHIPPED);

        // must not throw
        publisher.publishOrderStatusUpdated(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(STATUS_KEY + ".shipped"), eq(event));
    }

    private OrderCreatedEvent buildOrderCreatedEvent() {
        return new OrderCreatedEvent(
                1000L, 10L, "customer@example.com",
                new BigDecimal("500.00"),
                Order.OrderStatus.PENDING,
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                List.of(new OrderCreatedEvent.OrderItemEventInfo(
                        100L, "Handmade Vase", 2, new BigDecimal("250.00"), new BigDecimal("500.00")))
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
