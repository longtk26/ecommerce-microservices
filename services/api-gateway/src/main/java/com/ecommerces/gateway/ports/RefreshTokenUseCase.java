package com.ecommerces.gateway.ports;

import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import com.ecommerces.gateway.presentation.dto.RefreshTokenRequestDto;
import reactor.core.publisher.Mono;

public interface RefreshTokenUseCase {
    Mono<LoginResponseDto> refresh(RefreshTokenRequestDto request);
}
