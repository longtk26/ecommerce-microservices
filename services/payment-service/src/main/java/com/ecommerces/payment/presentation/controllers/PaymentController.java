package com.ecommerces.payment.presentation.controllers;

import com.ecommerces.payment.presentation.dto.ProcessPaymentRequestDto;
import com.ecommerces.payment.usecases.ProcessPaymentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void processPayment(@RequestBody ProcessPaymentRequestDto dto) {
        processPaymentUseCase.execute(dto);
    }
}
