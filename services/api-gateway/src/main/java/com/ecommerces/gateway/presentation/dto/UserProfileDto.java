package com.ecommerces.gateway.presentation.dto;

import java.util.List;

public record UserProfileDto(
        String id,
        String email,
        List<String> roles
) {}
