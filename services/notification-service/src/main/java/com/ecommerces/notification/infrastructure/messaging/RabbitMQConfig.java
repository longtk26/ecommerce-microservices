package com.ecommerces.notification.infrastructure.messaging;

import com.ecommerces.events.EventRoutes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /**
     * The single queue this service owns.
     * Named after the service so ownership is immediately obvious in the RabbitMQ UI.
     */
    public static final String QUEUE_ORDERS = "notification-service.orders";

    // ── Exchange (same topic exchange shared by all saga participants) ──────────

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    // ── Queue ──────────────────────────────────────────────────────────────────

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(QUEUE_ORDERS).build();
    }

    // ── Binding: wildcard catches ALL order.* events ───────────────────────────

    /**
     * Binds with {@code order.*} so this service automatically receives any
     * future routing keys like {@code order.delayed} or {@code order.refunded}
     * without any publisher needing to change.
     */
    @Bean
    public Binding ordersBinding(Queue ordersQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(ordersQueue)
                .to(sagaExchange)
                .with("order.*");   // wildcard binding
    }

    // ── JSON Message Converter ─────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
