package com.example.finnier.messaging;

import com.example.finnier.dto.event.OrderCreatedEvent;
import com.example.finnier.dto.event.OrderStatusUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    @RabbitListener(queues = "${rabbitmq.queue.order-created:order.created.queue}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent via RabbitMQ for Order ID: {}, Customer Email: {}, Total Amount: {}",
                event.orderId(), event.customerEmail(), event.totalAmount());

    }

    @RabbitListener(queues = "${rabbitmq.queue.order-status:order.status.queue}")
    public void handleOrderStatusUpdateEvent(OrderStatusUpdateEvent event) {
        log.info("Received OrderStatusUpdateEvent via RabbitMQ for Order ID: {}, Status changed from {} to {}",
                event.orderId(), event.previousStatus(), event.newStatus());
    }
}
