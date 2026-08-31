package com.ecommerces.gateway.presentation.dto;

public record LoginResponseDto(
        String accessToken,
        String idToken,
        String refreshToken,
        Integer expiresIn,
        String tokenType,
        UserProfileDto user
) {}
