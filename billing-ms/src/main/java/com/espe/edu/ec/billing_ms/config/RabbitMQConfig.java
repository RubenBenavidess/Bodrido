package com.espe.edu.ec.billing_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Notificaciones de órdenes (ESCUCHAMOS)
    public static final String ORDER_NOTIFICATION_EXCHANGE = "orders-notifications.exchange";
    public static final String BILLING_QUEUE_NAME = "billing-order-events.queue";
    public static final String ORDER_NOTIFICATION_ROUTING_KEY = "order.*";

    // Eventos de billing (PUBLICAMOS)
    public static final String BILLING_EXCHANGE_NAME = "billing-events.exchange";
    public static final String BILLING_ROUTING_KEY = "billing.*";

    @Bean
    public TopicExchange orderNotificationExchange() {
        return new TopicExchange(ORDER_NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue billingQueue() {
        return QueueBuilder.durable(BILLING_QUEUE_NAME).build();
    }

    @Bean
    public Binding billingBinding(Queue billingQueue, TopicExchange orderNotificationExchange) {
        return BindingBuilder.bind(billingQueue)
                .to(orderNotificationExchange)
                .with(ORDER_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE_NAME);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
