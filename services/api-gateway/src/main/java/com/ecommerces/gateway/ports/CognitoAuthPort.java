package com.ecommerces.gateway.ports;

import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import reactor.core.publisher.Mono;

public interface CognitoAuthPort {
    Mono<LoginResponseDto> initiateAuth(String email, String password);
    Mono<LoginResponseDto> refreshAuth(String refreshToken);
}
