package com.ecommerces.order.usecases;

import com.ecommerces.events.EventRoutes;
import com.ecommerces.events.OrderCreatedEvent;
import com.ecommerces.order.domain.Order;
import com.ecommerces.order.infrastructure.http.InventoryClient;
import com.ecommerces.order.infrastructure.http.dto.ProductInfo;
import com.ecommerces.order.ports.IMessageQueue;
import com.ecommerces.order.presentation.dto.CreateOrderRequestDto;
import com.ecommerces.order.presentation.dto.CreateOrderResponseDto;
import com.ecommerces.order.presentation.dto.OrderItemDto;
import com.ecommerces.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CreateOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderUseCase.class);

    private final IMessageQueue messageQueue;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public CreateOrderUseCase(IMessageQueue messageQueue,
                              OrderRepository orderRepository,
                              InventoryClient inventoryClient) {
        this.messageQueue = messageQueue;
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    /**
     * Creates a new order in PENDING status and kicks off the saga.
     *
     * <p><b>Transactional guarantee</b>: The {@code order.created} event is published
     * <em>after</em> the transaction commits (via {@link TransactionSynchronizationManager}).
     * This ensures the order row is visible in the DB before the Inventory Service
     * processes the event, preventing a race condition where inventory tries to
     * acknowledge an order that doesn't exist yet.
     *
     * @param dto the validated create-order request
     * @return response containing the new orderId and {@code "PENDING"} status
     */
    @Transactional
    public CreateOrderResponseDto execute(CreateOrderRequestDto dto) {
        return execute(dto, dto.getUserId());
    }

    @Transactional
    public CreateOrderResponseDto execute(CreateOrderRequestDto dto, String authenticatedUserId) {
        String effectiveUserId = (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId
                : dto.getUserId();

        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated user ID");
        }

        log.info("Creating order for userId={}, shopId={}, items={}",
                effectiveUserId, dto.getShopId(), dto.getItems().size());

        // 1. Fetch price snapshots from Inventory Service in a single bulk call
        //    (avoids N sequential HTTP requests — one per order item).
        List<String> productIds = dto.getItems().stream()
                .map(OrderItemDto::getProductId)
                .toList();

        Map<String, ProductInfo> productInfoByProductId = inventoryClient.getProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductInfo::productId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderCreatedEvent.OrderItemPayload> eventItems = new ArrayList<>();

        for (OrderItemDto itemDto : dto.getItems()) {
            ProductInfo info = productInfoByProductId.get(itemDto.getProductId());
            if (info == null) {
                throw new ResponseStatusException(HttpStatus.valueOf(422),
                        "Product not found in inventory: " + itemDto.getProductId());
            }
            BigDecimal lineTotal = info.unitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            eventItems.add(new OrderCreatedEvent.OrderItemPayload(
                    itemDto.getProductId(),
                    info.productName(),
                    info.unitPrice(),
                    itemDto.getQuantity()
            ));
        }

        // 2. Build and persist the order (CascadeType.ALL saves items automatically)
        Order order = new Order(effectiveUserId, UUID.fromString(dto.getShopId()), totalAmount);
        for (OrderCreatedEvent.OrderItemPayload payload : eventItems) {
            order.addItem(
                    UUID.fromString(payload.productId()),
                    payload.productName(),
                    payload.unitPrice(),
                    payload.quantity()
            );
        }
        Order saved = orderRepository.save(order);
        log.info("Order persisted: orderId={}, totalAmount={}", saved.getId(), saved.getTotalAmount());

        // 3. Publish order.created event AFTER transaction commits
        //    If the DB insert fails (exception thrown), this block never executes.
        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId().toString(),
                saved.getUserId(),
                saved.getShopId().toString(),
                eventItems,
                saved.getTotalAmount()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Publishing order.created event for orderId={}", event.orderId());
                messageQueue.publish(EventRoutes.EXCHANGE, EventRoutes.ORDER_CREATED, event);
            }
        });

        return new CreateOrderResponseDto(saved.getId().toString(), "PENDING", "Order placed. Processing...");
    }
}

