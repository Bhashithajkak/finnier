package com.example.finnier.messaging;

import com.example.finnier.dto.event.PaymentCompletedEvent;
import com.example.finnier.dto.event.PaymentFailedEvent;
import com.example.finnier.dto.event.PaymentRefundedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventListener {

    @RabbitListener(queues = "${rabbitmq.queue.payment-completed:payment.completed.queue}")
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent via RabbitMQ for Payment ID: {}, Order ID: {}, Amount: {}",
                event.paymentId(), event.orderId(), event.amount());
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-failed:payment.failed.queue}")
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        log.warn("Received PaymentFailedEvent via RabbitMQ for Payment ID: {}, Order ID: {}, Reason: {}",
                event.paymentId(), event.orderId(), event.failureReason());
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-refunded:payment.refunded.queue}")
    public void handlePaymentRefundedEvent(PaymentRefundedEvent event) {
        log.info("Received PaymentRefundedEvent via RabbitMQ for Payment ID: {}, Order ID: {}, Refund Amount: {}",
                event.paymentId(), event.orderId(), event.refundAmount());
    }
}
