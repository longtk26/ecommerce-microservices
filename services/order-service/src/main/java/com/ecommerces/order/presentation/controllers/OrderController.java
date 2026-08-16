package com.ecommerces.order.presentation.controllers;

import com.ecommerces.order.domain.Order;
import com.ecommerces.order.presentation.dto.CreateOrderRequestDto;
import com.ecommerces.order.presentation.dto.CreateOrderResponseDto;
import com.ecommerces.order.presentation.dto.GetOrderResponseDto;
import com.ecommerces.order.presentation.dto.GetOrderResponseDto.GetOrderItemDto;
import com.ecommerces.order.repository.OrderRepository;
import com.ecommerces.order.usecases.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderRepository orderRepository;

    public OrderController(CreateOrderUseCase createOrderUseCase, OrderRepository orderRepository) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponseDto createOrder(@Valid @RequestBody CreateOrderRequestDto dto) {
        return createOrderUseCase.execute(dto);
    }

    /**
     * Internal endpoint consumed by payment-service to fetch order details
     * (userId, totalAmount, items) without trusting the frontend payload.
     */
    @GetMapping("/{orderId}")
    public GetOrderResponseDto getOrder(@PathVariable String orderId) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));

        List<GetOrderItemDto> items = order.getItems().stream()
                .map(i -> new GetOrderItemDto(
                        i.getProductId().toString(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantity()))
                .toList();

        return new GetOrderResponseDto(
                order.getId().toString(),
                order.getUserId(),
                order.getShopId().toString(),
                order.getStatus().name(),
                order.getTotalAmount(),
                items);
    }
}