package com.ecommerces.inventory.ports;

public interface IMessageQueue {
    void publish(String exchange, String routingKey, Object message);
}
