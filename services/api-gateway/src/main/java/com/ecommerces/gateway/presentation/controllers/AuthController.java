package com.ecommerces.gateway.presentation.controllers;

import com.ecommerces.gateway.ports.AuthenticateUseCase;
import com.ecommerces.gateway.ports.RefreshTokenUseCase;
import com.ecommerces.gateway.presentation.dto.LoginRequestDto;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import com.ecommerces.gateway.presentation.dto.RefreshTokenRequestDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(AuthenticateUseCase authenticateUseCase, RefreshTokenUseCase refreshTokenUseCase) {
        this.authenticateUseCase = authenticateUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("[GATEWAY-LOGIN] Step 1.1: AuthController.login() received login request for email: {}", request.email());
        return authenticateUseCase.authenticate(request)
                .doOnSuccess(res -> log.info("[GATEWAY-LOGIN] Step 1.5: AuthController.login() successfully completed for user: {}", res.user() != null ? res.user().email() : "unknown"))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponseDto>> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("[GATEWAY-AUTH] AuthController.refresh() received token refresh request");
        return refreshTokenUseCase.refresh(request)
                .map(ResponseEntity::ok);
    }
}
