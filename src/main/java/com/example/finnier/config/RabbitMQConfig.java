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
