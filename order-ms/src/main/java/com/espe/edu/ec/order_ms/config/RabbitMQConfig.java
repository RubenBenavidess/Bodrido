package com.espe.edu.ec.order_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Notificaciones internas
    public static final String EXCHANGE_NAME = "orders-notifications.exchange";
    public static final String QUEUE_NAME = "orders-notifications.queue";
    public static final String ROUTING_KEY = "orders-notifications.routingKey";

    // Validaciones desde otros microservicios
    public static final String VALIDATION_EXCHANGE_NAME = "orders-validations.exchange";
    public static final String VALIDATION_QUEUE_NAME = "orders-validations.queue";
    public static final String VALIDATION_ROUTING_KEY = "orders-validations.routingKey";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding notificationBinding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public TopicExchange validationExchange() {
        return new TopicExchange(VALIDATION_EXCHANGE_NAME);
    }

    @Bean
    public Queue validationQueue() {
        return QueueBuilder.durable(VALIDATION_QUEUE_NAME).build();
    }

    @Bean
    public Binding validationBinding(Queue validationQueue, TopicExchange validationExchange) {
        return BindingBuilder.bind(validationQueue)
                .to(validationExchange)
                .with(VALIDATION_ROUTING_KEY);
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
