package com.ecommerces.order.usecases;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.order.ports.IMessageQueue;
import com.ecommerces.order.presentation.dto.CreateOrderRequestDto;
import org.springframework.stereotype.Service;

@Service
public class CreateOrderUseCase {

    private final IMessageQueue messageQueue;

    public CreateOrderUseCase(IMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public void execute(CreateOrderRequestDto dto) {
        messageQueue.publish(EventRoutes.EXCHANGE, EventRoutes.PAYMENT_PROCESSED, dto);
    }
}
