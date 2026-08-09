package com.example.finnier.messaging;

import com.example.finnier.dto.event.OrderCreatedEvent;
import com.example.finnier.dto.event.OrderStatusUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.order:order.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.order-created:order.created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.order-status:order.status}")
    private String orderStatusRoutingKey;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("Publishing OrderCreatedEvent for orderId: {} to exchange: {}", event.orderId(), exchangeName);
            rabbitTemplate.convertAndSend(exchangeName, orderCreatedRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for orderId: {}", event.orderId(), e);
        }
    }

    public void publishOrderStatusUpdated(OrderStatusUpdateEvent event) {
        try {
            String routingKey = orderStatusRoutingKey + "." + event.newStatus().name().toLowerCase();
            log.info("Publishing OrderStatusUpdateEvent for orderId: {} with routingKey: {}", event.orderId(), routingKey);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish OrderStatusUpdateEvent for orderId: {}", event.orderId(), e);
        }
    }
}
