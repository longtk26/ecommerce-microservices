package com.ecommerces.order.infrastructure.message_queue.rabbitmq;

import com.ecommerces.events.EventRoutes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queues this service consumes from
    public static final String QUEUE_PAYMENT_PROCESSED = "order-service.payment-processed";
    public static final String QUEUE_INVENTORY_FAILED = "order-service.inventory-failed";
    public static final String QUEUE_INVENTORY_RELEASED = "order-service.inventory-released";

    // ── Exchange ────────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    // ── Queues (durable = survive broker restart) ───────────────────────────────

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_PROCESSED).build();
    }

    @Bean
    public Queue inventoryFailedQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY_FAILED).build();
    }

    @Bean
    public Queue inventoryReleasedQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY_RELEASED).build();
    }

    // ── Bindings (queue ← routing key ← exchange) ──────────────────────────────

    @Bean
    public Binding paymentProcessedBinding(Queue paymentProcessedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentProcessedQueue)
                .to(sagaExchange)
                .with(EventRoutes.PAYMENT_PROCESSED);
    }

    @Bean
    public Binding inventoryFailedBinding(Queue inventoryFailedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(inventoryFailedQueue)
                .to(sagaExchange)
                .with(EventRoutes.INVENTORY_FAILED);
    }

    @Bean
    public Binding inventoryReleasedBinding(Queue inventoryReleasedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(inventoryReleasedQueue)
                .to(sagaExchange)
                .with(EventRoutes.INVENTORY_RELEASED);
    }

    // ── JSON Converter + RabbitTemplate (publisher side) ───────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
