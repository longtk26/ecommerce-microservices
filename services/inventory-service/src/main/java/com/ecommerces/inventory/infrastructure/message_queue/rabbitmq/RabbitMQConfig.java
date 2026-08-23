package com.ecommerces.inventory.infrastructure.message_queue.rabbitmq;

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
    public static final String QUEUE_ORDER_CREATED = "inventory-service.order-created";
    public static final String QUEUE_PAYMENT_PROCESSED = "inventory-service.payment-processed";
    public static final String QUEUE_PAYMENT_FAILED = "inventory-service.payment-failed";

    // ── Exchange ─────────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    // ── Queues (durable = survive broker restart) ─────────────────────────────

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED).build();
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_PROCESSED).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_FAILED).build();
    }

    // ── Bindings (queue ← routing key ← exchange) ────────────────────────────

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(sagaExchange)
                .with(EventRoutes.ORDER_CREATED);
    }

    @Bean
    public Binding paymentProcessedBinding(Queue paymentProcessedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentProcessedQueue)
                .to(sagaExchange)
                .with(EventRoutes.PAYMENT_PROCESSED);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentFailedQueue)
                .to(sagaExchange)
                .with(EventRoutes.PAYMENT_FAILED);
    }

    // ── JSON Converter + RabbitTemplate (publisher side) ─────────────────────

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
