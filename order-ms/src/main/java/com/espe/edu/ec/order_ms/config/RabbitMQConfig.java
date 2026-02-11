package com.espe.edu.ec.order_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Notificaciones internas
    public static final String EXCHANGE_NAME = "orders-notifications.exchange";
    public static final String QUEUE_NAME = "orders-notifications.queue";
    public static final String ROUTING_KEY = "orders-notifications.routingKey";



    // Publicar órdenes creadas para verificación en customer-ms
    public static final String ORDERS_VALIDATIONS_CUSTOMER_EXCHANGE = "orders-validations-customer.exchange";
    public static final String ORDERS_VALIDATIONS_CUSTOMER_ROUTING_KEY = "orders.validations.customer";

    public static final String ORDER_VERIFICATION_RESULT_EXCHANGE = "order-verification-result.exchange";
    public static final String ORDER_VERIFICATION_RESULT_QUEUE = "order.verification.result";
    public static final String ORDER_VERIFICATION_RESULT_ROUTING_KEY = "order.verification.result";

    // Publicar órdenes para validar asignación en fleet-ms
    public static final String ORDERS_VALIDATIONS_FLEET_EXCHANGE = "orders-validations-fleet.exchange";
    public static final String ORDERS_VALIDATIONS_FLEET_ROUTING_KEY = "orders.validations.fleet";

    public static final String ORDER_FLEET_VERIFICATION_RESULT_EXCHANGE = "order-fleet-verification-result.exchange";
    public static final String ORDER_FLEET_VERIFICATION_RESULT_QUEUE = "order-fleet.verification.result";
    public static final String ORDER_FLEET_VERIFICATION_RESULT_ROUTING_KEY = "order-fleet.verification.result";;

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding notificationBinding(
            @Qualifier("notificationQueue") Queue queue, 
            @Qualifier("notificationExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROUTING_KEY);
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

    // ==================== ORDERS VALIDATIONS CUSTOMER EXCHANGE ====================
    @Bean
    public TopicExchange ordersValidationsCustomerExchange() {
        return new TopicExchange(ORDERS_VALIDATIONS_CUSTOMER_EXCHANGE, true, false);
    }

    // ==================== ORDER VERIFICATION RESULT EXCHANGE ====================
    @Bean
    public TopicExchange orderVerificationResultExchange() {
        return new TopicExchange(ORDER_VERIFICATION_RESULT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderVerificationResultQueue() {
        return QueueBuilder.durable(ORDER_VERIFICATION_RESULT_QUEUE).build();
    }

    @Bean
    public Binding orderVerificationResultBinding(
            @Qualifier("orderVerificationResultQueue") Queue queue,
            @Qualifier("orderVerificationResultExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ORDER_VERIFICATION_RESULT_ROUTING_KEY);
    }

    // ==================== FLEET VERIFICATION RESULT EXCHANGE ====================
    @Bean
    public TopicExchange orderFleetVerificationResultExchange() {
        return new TopicExchange(ORDER_FLEET_VERIFICATION_RESULT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderFleetVerificationResultQueue() {
        return QueueBuilder.durable(ORDER_FLEET_VERIFICATION_RESULT_QUEUE).build();
    }

    @Bean
    public Binding orderFleetVerificationResultBinding(
            @Qualifier("orderFleetVerificationResultQueue") Queue queue,
            @Qualifier("orderFleetVerificationResultExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ORDER_FLEET_VERIFICATION_RESULT_ROUTING_KEY);
    }

    // ==================== FLEET VALIDATIONS EXCHANGE ====================
    @Bean
    public TopicExchange ordersValidationsFleetExchange() {
        return new TopicExchange(ORDERS_VALIDATIONS_FLEET_EXCHANGE, true, false);
    }
}
