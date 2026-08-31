package com.ecommerces.gateway.usecases;

import com.ecommerces.gateway.ports.CognitoAuthPort;
import com.ecommerces.gateway.ports.RefreshTokenUseCase;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import com.ecommerces.gateway.presentation.dto.RefreshTokenRequestDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final CognitoAuthPort cognitoAuthPort;

    public RefreshTokenUseCaseImpl(CognitoAuthPort cognitoAuthPort) {
        this.cognitoAuthPort = cognitoAuthPort;
    }

    @Override
    public Mono<LoginResponseDto> refresh(RefreshTokenRequestDto request) {
        return cognitoAuthPort.refreshAuth(request.refreshToken());
    }
}
