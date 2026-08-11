package com.ecommerces.payment.infrastructure.message_queue.rabbitmq;

import com.ecommerces.events.EventRoutes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queues this service consumes from
    public static final String QUEUE_INVENTORY_RESERVED = "payment-service.inventory-reserved";

    // ── Exchange ─────────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EventRoutes.EXCHANGE, true, false);
    }

    // ── Queues (durable = survive broker restart) ─────────────────────────────

    @Bean
    public Queue inventoryReservedQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY_RESERVED).build();
    }

    // ── Bindings (queue ← routing key ← exchange) ────────────────────────────

    @Bean
    public Binding inventoryReservedBinding(Queue inventoryReservedQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(inventoryReservedQueue)
                .to(sagaExchange)
                .with(EventRoutes.INVENTORY_RESERVED);
    }

    // ── JSON Converter + RabbitTemplate (publisher side) ─────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
