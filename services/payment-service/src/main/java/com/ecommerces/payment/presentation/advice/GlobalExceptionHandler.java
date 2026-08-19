package com.ecommerces.payment.presentation.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerces.payment.domain.exceptions.PaymentFailedException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for the Payment Service REST layer.
 * Returns RFC 9457 {@link ProblemDetail} responses for clean, consistent error
 * bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean validation failures (e.g. missing required fields, invalid
     * quantities).
     * Returns 400 Bad Request with a map of field → error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing // keep first error per field
                ));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:order-service:validation-error"));
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more fields failed validation.");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * Handles {@link ResponseStatusException} thrown by e.g.
     * {@link com.ecommerces.order.infrastructure.http.InventoryClient}
     * when the Inventory Service is unreachable or returns an error.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatusCode());
        problem.setType(URI.create("urn:order-service:upstream-error"));
        problem.setTitle("Request Error");
        problem.setDetail(ex.getReason());
        return problem;
    }

    /**
     * Catch-all for unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("urn:order-service:internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again later.");
        return problem;
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ProblemDetail handlePaymentFailedException(PaymentFailedException ex) {
        log.warn("PaymentFailedException: reason={}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:payment-service:payment-failed"));
        problem.setTitle("Payment Failed");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
