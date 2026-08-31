package com.ecommerces.gateway.ports;

import com.ecommerces.gateway.presentation.dto.LoginRequestDto;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import reactor.core.publisher.Mono;

public interface AuthenticateUseCase {
    Mono<LoginResponseDto> authenticate(LoginRequestDto request);
}
