package com.ecommerces.gateway.usecases;

import com.ecommerces.gateway.ports.AuthenticateUseCase;
import com.ecommerces.gateway.ports.CognitoAuthPort;
import com.ecommerces.gateway.presentation.dto.LoginRequestDto;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthenticateUseCaseImpl implements AuthenticateUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateUseCaseImpl.class);
    private final CognitoAuthPort cognitoAuthPort;

    public AuthenticateUseCaseImpl(CognitoAuthPort cognitoAuthPort) {
        this.cognitoAuthPort = cognitoAuthPort;
    }

    @Override
    public Mono<LoginResponseDto> authenticate(LoginRequestDto request) {
        log.info("[GATEWAY-LOGIN] Step 1.2: AuthenticateUseCaseImpl.authenticate() delegating to CognitoAuthPort for email: {}", request.email());
        return cognitoAuthPort.initiateAuth(request.email(), request.password());
    }
}
