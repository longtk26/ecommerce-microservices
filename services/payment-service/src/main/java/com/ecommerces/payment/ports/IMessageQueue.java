package com.ecommerces.payment.ports;

public interface IMessageQueue {
    void publish(String exchange, String routingKey, Object message);
}
