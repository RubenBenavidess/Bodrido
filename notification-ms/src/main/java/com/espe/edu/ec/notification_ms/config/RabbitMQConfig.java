package com.espe.edu.ec.notification_ms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ======================== Order Notifications ========================
    public static final String ORDERS_NOTIFICATIONS_EXCHANGE = "orders-notifications.exchange";
    public static final String ORDERS_NOTIFICATIONS_QUEUE = "orders-notifications.queue";
    public static final String ORDERS_NOTIFICATIONS_ROUTING_KEY = "orders-notifications.routingKey";




    // ======================== Billing Events ========================
    public static final String BILLING_EVENTS_EXCHANGE = "billing-events.exchange";
    public static final String BILLING_NOTIFICATIONS_QUEUE = "notification-billing-events.queue";
    public static final String BILLING_ROUTING_KEY = "billing.*";

    // ======================== Order Validations ========================
    public static final String ORDER_VALIDATIONS_EXCHANGE = "orders-validations.exchange";
    public static final String VALIDATIONS_NOTIFICATIONS_QUEUE = "notification-validations-events.queue";
    public static final String VALIDATIONS_ROUTING_KEY = "validation.*";

    // ======================== Billing Events Configuration ========================


    // ======================== Order Notifications Configuration ========================
    @Bean
    public TopicExchange ordersNotificationsExchange() {
        return new TopicExchange(ORDERS_NOTIFICATIONS_EXCHANGE, true, false);
    }
    @Bean
    public Queue ordersNotificationsQueue() {
        return QueueBuilder.durable(ORDERS_NOTIFICATIONS_QUEUE).build();
    }
    @Bean
    public Binding ordersNotificationsBinding(
            Queue ordersNotificationsQueue,
            TopicExchange ordersNotificationsExchange) {

        return BindingBuilder.bind(ordersNotificationsQueue)
                .to(ordersNotificationsExchange)
                .with(ORDERS_NOTIFICATIONS_ROUTING_KEY);
    }



    @Bean
    public TopicExchange billingEventsExchange() {
        return new TopicExchange(BILLING_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue billingNotificationsQueue() {
        return QueueBuilder.durable(BILLING_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Binding billingNotificationsBinding(Queue billingNotificationsQueue, TopicExchange billingEventsExchange) {
        return BindingBuilder.bind(billingNotificationsQueue)
                .to(billingEventsExchange)
                .with(BILLING_ROUTING_KEY);
    }

    // ======================== Order Validations Configuration ========================
    @Bean
    public TopicExchange orderValidationsExchange() {
        return new TopicExchange(ORDER_VALIDATIONS_EXCHANGE, true, false);
    }

    @Bean
    public Queue validationsNotificationsQueue() {
        return QueueBuilder.durable(VALIDATIONS_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Binding validationsNotificationsBinding(Queue validationsNotificationsQueue, TopicExchange orderValidationsExchange) {
        return BindingBuilder.bind(validationsNotificationsQueue)
                .to(orderValidationsExchange)
                .with(VALIDATIONS_ROUTING_KEY);
    }

    // ======================== Message Converter ========================
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
