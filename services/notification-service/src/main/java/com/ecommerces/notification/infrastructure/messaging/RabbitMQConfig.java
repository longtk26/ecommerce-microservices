package com.ecommerces.notification.infrastructure.messaging;

import com.ecommerces.events.EventRoutes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Unique durable queue for notification-service
    public static final String QUEUE_ORDERS = "notification-service.orders";

    // ── Exchange (same topic exchange shared by all saga participants) ──────────

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    // ── Queue ───────────────────────────────────────────────────────────────────

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(QUEUE_ORDERS).build();
    }

    // ── Binding: wildcard catches ALL order.* events ───────────────────────────

    @Bean
    public Binding ordersBinding(Queue ordersQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(ordersQueue)
                .to(sagaExchange)
                .with("order.*");
    }

    // ── JSON Message Converter & ObjectMapper ──────────────────────────────────

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
