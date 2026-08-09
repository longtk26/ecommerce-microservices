
package com.ecommerces.order.ports;

public interface IMessageQueue {
    void publish(String exchange, String routingKey, Object message);
}
