package com.example.finnier.messaging;

import com.example.finnier.dto.event.PaymentCompletedEvent;
import com.example.finnier.dto.event.PaymentFailedEvent;
import com.example.finnier.dto.event.PaymentRefundedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.order:order.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.payment-completed:payment.completed}")
    private String paymentCompletedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-refunded:payment.refunded}")
    private String paymentRefundedRoutingKey;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("Publishing PaymentCompletedEvent for paymentId: {}, orderId: {}", event.paymentId(), event.orderId());
            rabbitTemplate.convertAndSend(exchangeName, paymentCompletedRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent for paymentId: {}", event.paymentId(), e);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            log.info("Publishing PaymentFailedEvent for paymentId: {}, orderId: {}", event.paymentId(), event.orderId());
            rabbitTemplate.convertAndSend(exchangeName, paymentFailedRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent for paymentId: {}", event.paymentId(), e);
        }
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        try {
            log.info("Publishing PaymentRefundedEvent for paymentId: {}, orderId: {}", event.paymentId(), event.orderId());
            rabbitTemplate.convertAndSend(exchangeName, paymentRefundedRoutingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish PaymentRefundedEvent for paymentId: {}", event.paymentId(), e);
        }
    }
}
