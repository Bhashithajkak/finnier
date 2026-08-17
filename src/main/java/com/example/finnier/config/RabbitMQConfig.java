package com.example.finnier.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.order:order.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.order-created:order.created.queue}")
    private String orderCreatedQueueName;

    @Value("${rabbitmq.queue.order-status:order.status.queue}")
    private String orderStatusQueueName;

    @Value("${rabbitmq.routing-key.order-created:order.created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.order-status:order.status}")
    private String orderStatusRoutingKey;

    @Value("${rabbitmq.queue.payment-completed:payment.completed.queue}")
    private String paymentCompletedQueueName;

    @Value("${rabbitmq.queue.payment-failed:payment.failed.queue}")
    private String paymentFailedQueueName;

    @Value("${rabbitmq.queue.payment-refunded:payment.refunded.queue}")
    private String paymentRefundedQueueName;

    @Value("${rabbitmq.routing-key.payment-completed:payment.completed}")
    private String paymentCompletedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-refunded:payment.refunded}")
    private String paymentRefundedRoutingKey;

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueueName).build();
    }

    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(orderStatusQueueName).build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(paymentCompletedQueueName).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueueName).build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable(paymentRefundedQueueName).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderStatusQueue)
                .to(orderExchange)
                .with(orderStatusRoutingKey + ".#");
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(paymentCompletedQueue)
                .to(orderExchange)
                .with(paymentCompletedRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(paymentFailedQueue)
                .to(orderExchange)
                .with(paymentFailedRoutingKey);
    }

    @Bean
    public Binding paymentRefundedBinding(Queue paymentRefundedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(paymentRefundedQueue)
                .to(orderExchange)
                .with(paymentRefundedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
