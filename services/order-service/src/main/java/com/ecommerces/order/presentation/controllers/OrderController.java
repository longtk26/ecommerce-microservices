package com.ecommerces.order.presentation.controllers;

import com.ecommerces.order.presentation.dto.CreateOrderRequestDto;
import com.ecommerces.order.presentation.dto.CreateOrderResponseDto;
import com.ecommerces.order.usecases.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponseDto createOrder(@Valid @RequestBody CreateOrderRequestDto dto) {
        return createOrderUseCase.execute(dto);
    }
}