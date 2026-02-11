package com.espe.edu.ec.customer_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Cola principal para validación de clientes
    public static final String CUSTOMER_VALIDATION_EXCHANGE = "customers-validation.exchange";
    public static final String CUSTOMER_VALIDATION_QUEUE = "customers.validation";
    public static final String CUSTOMER_VALIDATION_ROUTING_KEY = "customers.validation.routing";

    // Cola Dead Letter para reintentos
    public static final String CUSTOMER_VALIDATION_DLX = "customers-validation.dlx";
    public static final String CUSTOMER_VALIDATION_DLQ = "customers.validation.dlq";
    public static final String CUSTOMER_VALIDATION_DLK = "customers.validation.dlk";
    
    // Exchange para compensación (rollback de usuario)
    public static final String ROLLBACK_EXCHANGE = "user-rollback.exchange";
    public static final String ROLLBACK_QUEUE = "user.rollback";
    public static final String ROLLBACK_ROUTING_KEY = "user.rollback.routing";

    // Exchange para verificación de órdenes desde order-ms
    public static final String ORDERS_VALIDATIONS_CUSTOMER_EXCHANGE = "orders-validations-customer.exchange";
    public static final String ORDERS_VALIDATIONS_CUSTOMER_QUEUE = "orders.validations.customer";
    public static final String ORDERS_VALIDATIONS_CUSTOMER_ROUTING_KEY = "orders.validations.customer";

    // Exchange para respuesta de verificación
    public static final String ORDER_VERIFICATION_RESULT_EXCHANGE = "order-verification-result.exchange";
    public static final String ORDER_VERIFICATION_RESULT_ROUTING_KEY = "order.verification.result";

    // ==================== DRIVER USER CREATED (auth-ms → customer-ms) ====================
    // Exchange/Queue dedicado para recibir creación de usuarios DRIVER desde auth-ms
    // SEPARADO del flujo de validación de FleetService
    public static final String DRIVER_USER_CREATED_EXCHANGE = "driver-user-created.exchange";
    public static final String DRIVER_USER_CREATED_QUEUE = "driver.user.created";
    public static final String DRIVER_USER_CREATED_ROUTING_KEY = "driver.user.created.routing";

    // Dead Letter para driver user created
    public static final String DRIVER_USER_CREATED_DLX = "driver-user-created.dlx";
    public static final String DRIVER_USER_CREATED_DLQ = "driver.user.created.dlq";
    public static final String DRIVER_USER_CREATED_DLK = "driver.user.created.dlk";

    @Bean
    public TopicExchange customerValidationExchange() {
        return new TopicExchange(CUSTOMER_VALIDATION_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange customerValidationDlx() {
        return new TopicExchange(CUSTOMER_VALIDATION_DLX, true, false);
    }

    @Bean
    public Queue customerValidationDlq() {
        return QueueBuilder.durable(CUSTOMER_VALIDATION_DLQ)
                .build();
    }

    @Bean
    public Binding customerValidationDlBinding(
            @Qualifier("customerValidationDlq") Queue dlq,
            @Qualifier("customerValidationDlx") TopicExchange dlx) {
        return BindingBuilder.bind(dlq)
                .to(dlx)
                .with(CUSTOMER_VALIDATION_DLK);
    }

    @Bean
    public Queue customerValidationQueue() {
        return QueueBuilder.durable(CUSTOMER_VALIDATION_QUEUE)
                .deadLetterExchange(CUSTOMER_VALIDATION_DLX)
                .deadLetterRoutingKey(CUSTOMER_VALIDATION_DLK)
                .ttl(3600000) // 1 hora TTL
                .build();
    }

    @Bean
    public Binding customerValidationBinding(
            @Qualifier("customerValidationQueue") Queue queue,
            @Qualifier("customerValidationExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(CUSTOMER_VALIDATION_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange rollbackExchange() {
        return new TopicExchange(ROLLBACK_EXCHANGE, true, false);
    }

    @Bean
    public Queue rollbackQueue() {
        return QueueBuilder.durable(ROLLBACK_QUEUE).build();
    }

    @Bean
    public Binding rollbackBinding(
            @Qualifier("rollbackQueue") Queue queue,
            @Qualifier("rollbackExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ROLLBACK_ROUTING_KEY);
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

    @Bean
    public Queue ordersValidationsCustomerQueue() {
        return QueueBuilder.durable(ORDERS_VALIDATIONS_CUSTOMER_QUEUE).build();
    }

    @Bean
    public Binding ordersValidationsCustomerBinding(
            @Qualifier("ordersValidationsCustomerQueue") Queue queue,
            @Qualifier("ordersValidationsCustomerExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(ORDERS_VALIDATIONS_CUSTOMER_ROUTING_KEY);
    }

    // ==================== ORDEN VERIFICATION RESULT EXCHANGE ====================
    @Bean
    public TopicExchange orderVerificationResultExchange() {
        return new TopicExchange(ORDER_VERIFICATION_RESULT_EXCHANGE, true, false);
    }

    // ==================== DRIVER VALIDATION EXCHANGES (desde FleetService) ====================
    // Cuando FleetService crea un driver, valida la existencia del usuario
    public static final String DRIVER_VALIDATION_EXCHANGE = "drivers-validation.exchange";
    public static final String DRIVER_VALIDATION_QUEUE = "drivers.validation";
    public static final String DRIVER_VALIDATION_ROUTING_KEY = "drivers.validation.routing";

    // Respuesta de validación de driver de vuelta a FleetService
    public static final String DRIVER_VALIDATION_RESULT_EXCHANGE = "driver-validation-result.exchange";
    public static final String DRIVER_VALIDATION_RESULT_ROUTING_KEY = "driver.validation.result";

    // Cola Dead Letter para drivers
    public static final String DRIVER_VALIDATION_DLX = "drivers-validation.dlx";
    public static final String DRIVER_VALIDATION_DLQ = "drivers.validation.dlq";
    public static final String DRIVER_VALIDATION_DLK = "drivers.validation.dlk";

    // Compensación de drivers
    public static final String DRIVER_COMPENSATION_EXCHANGE = "driver-compensation.exchange";
    public static final String DRIVER_COMPENSATION_ROUTING_KEY = "driver.compensation.routing";

    @Bean
    public TopicExchange driverValidationExchange() {
        return new TopicExchange(DRIVER_VALIDATION_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange driverValidationDlx() {
        return new TopicExchange(DRIVER_VALIDATION_DLX, true, false);
    }

    @Bean
    public Queue driverValidationDlq() {
        return QueueBuilder.durable(DRIVER_VALIDATION_DLQ).build();
    }

    @Bean
    public Binding driverValidationDlBinding(
            @Qualifier("driverValidationDlq") Queue dlq,
            @Qualifier("driverValidationDlx") TopicExchange dlx) {
        return BindingBuilder.bind(dlq)
                .to(dlx)
                .with(DRIVER_VALIDATION_DLK);
    }

    @Bean
    public Queue driverValidationQueue() {
        return QueueBuilder.durable(DRIVER_VALIDATION_QUEUE)
                .deadLetterExchange(DRIVER_VALIDATION_DLX)
                .deadLetterRoutingKey(DRIVER_VALIDATION_DLK)
                .ttl(3600000) // 1 hora TTL
                .build();
    }

    @Bean
    public Binding driverValidationBinding(
            @Qualifier("driverValidationQueue") Queue queue,
            @Qualifier("driverValidationExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(DRIVER_VALIDATION_ROUTING_KEY);
    }

    @Bean
    public TopicExchange driverValidationResultExchange() {
        return new TopicExchange(DRIVER_VALIDATION_RESULT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange driverCompensationExchange() {
        return new TopicExchange(DRIVER_COMPENSATION_EXCHANGE, true, false);
    }

    // ==================== DRIVER USER CREATED BEANS ====================
    @Bean
    public TopicExchange driverUserCreatedExchange() {
        return new TopicExchange(DRIVER_USER_CREATED_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange driverUserCreatedDlx() {
        return new TopicExchange(DRIVER_USER_CREATED_DLX, true, false);
    }

    @Bean
    public Queue driverUserCreatedDlq() {
        return QueueBuilder.durable(DRIVER_USER_CREATED_DLQ).build();
    }

    @Bean
    public Binding driverUserCreatedDlBinding(
            @Qualifier("driverUserCreatedDlq") Queue dlq,
            @Qualifier("driverUserCreatedDlx") TopicExchange dlx) {
        return BindingBuilder.bind(dlq)
                .to(dlx)
                .with(DRIVER_USER_CREATED_DLK);
    }

    @Bean
    public Queue driverUserCreatedQueue() {
        return QueueBuilder.durable(DRIVER_USER_CREATED_QUEUE)
                .deadLetterExchange(DRIVER_USER_CREATED_DLX)
                .deadLetterRoutingKey(DRIVER_USER_CREATED_DLK)
                .ttl(3600000) // 1 hora TTL
                .build();
    }

    @Bean
    public Binding driverUserCreatedBinding(
            @Qualifier("driverUserCreatedQueue") Queue queue,
            @Qualifier("driverUserCreatedExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(DRIVER_USER_CREATED_ROUTING_KEY);
    }
}
