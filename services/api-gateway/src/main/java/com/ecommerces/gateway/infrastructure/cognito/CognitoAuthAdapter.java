package com.ecommerces.gateway.infrastructure.cognito;

import com.ecommerces.gateway.ports.CognitoAuthPort;
import com.ecommerces.gateway.presentation.dto.LoginResponseDto;
import com.ecommerces.gateway.presentation.dto.UserProfileDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CognitoAuthAdapter implements CognitoAuthPort {

    private static final Logger log = LoggerFactory.getLogger(CognitoAuthAdapter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CognitoIdentityProviderAsyncClient cognitoClient;
    private final String clientId;
    private final String clientSecret;

    public CognitoAuthAdapter(
            @Value("${cognito.client-id:${COGNITO_CLIENT_ID:}}") String clientId,
            @Value("${cognito.client-secret:${COGNITO_CLIENT_SECRET:}}") String clientSecret,
            @Value("${aws.region:${AWS_REGION:us-east-1}}") String awsRegion) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cognitoClient = CognitoIdentityProviderAsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();
        log.info("CognitoAuthAdapter initialized with region: {}, clientId configured: {}, clientSecret configured: {}",
                awsRegion, !clientId.isBlank(), (clientSecret != null && !clientSecret.isBlank()));
    }

    @Override
    public Mono<LoginResponseDto> initiateAuth(String email, String password) {
        if (clientId == null || clientId.isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "COGNITO_CLIENT_ID is not configured in environment"
            ));
        }

        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", email);
        authParams.put("PASSWORD", password);

        String secretHash = calculateSecretHash(email);
        if (secretHash != null) {
            authParams.put("SECRET_HASH", secretHash);
        }

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        log.info("[GATEWAY-LOGIN] Step 1.3: CognitoAuthAdapter.initiateAuth() calling AWS Cognito InitiateAuth for username: {}", email);

        return Mono.fromFuture(cognitoClient.initiateAuth(authRequest))
                .map(response -> {
                    AuthenticationResultType result = response.authenticationResult();
                    if (result == null) {
                        String challenge = response.challengeNameAsString();
                        log.warn("[GATEWAY-LOGIN] Cognito returned challenge '{}' for user: {}", challenge, email);
                        if ("NEW_PASSWORD_REQUIRED".equals(challenge)) {
                            throw new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Temporary password detected (NEW_PASSWORD_REQUIRED). Please set a permanent password in the AWS Cognito Console."
                            );
                        }
                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authentication challenge required: " + (challenge != null ? challenge : "UNKNOWN")
                        );
                    }

                    String accessToken = result.accessToken();
                    String idToken = result.idToken();
                    String refreshToken = result.refreshToken();
                    Integer expiresIn = result.expiresIn();
                    String tokenType = result.tokenType() != null ? result.tokenType() : "Bearer";

                    UserProfileDto profile = extractUserProfile(idToken != null ? idToken : accessToken, email);
                    log.info("[GATEWAY-LOGIN] Step 1.4: CognitoAuthAdapter.initiateAuth() tokens generated successfully for user: {}, roles: {}", email, profile.roles());

                    return new LoginResponseDto(
                            accessToken,
                            idToken,
                            refreshToken,
                            expiresIn,
                            tokenType,
                            profile
                    );
                })
                .doOnError(err -> log.error("[GATEWAY-LOGIN] Cognito authentication failed for {}: {}", email, err.getMessage()))
                .onErrorMap(err -> {
                    Throwable cause = err;
                    while (cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException) {
                        if (cause.getCause() != null) {
                            cause = cause.getCause();
                        } else {
                            break;
                        }
                    }

                    if (cause instanceof UserNotFoundException || cause instanceof NotAuthorizedException) {
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
                    } else if (cause instanceof UserNotConfirmedException) {
                        return new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is not confirmed");
                    } else if (cause instanceof ResponseStatusException) {
                        return cause;
                    }
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed: " + cause.getMessage());
                });
    }

    @Override
    public Mono<LoginResponseDto> refreshAuth(String refreshToken) {
        if (clientId == null || clientId.isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "COGNITO_CLIENT_ID is not configured in environment"
            ));
        }

        Map<String, String> authParams = new HashMap<>();
        authParams.put("REFRESH_TOKEN", refreshToken);

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        return Mono.fromFuture(cognitoClient.initiateAuth(authRequest))
                .map(response -> {
                    AuthenticationResultType result = response.authenticationResult();
                    String accessToken = result.accessToken();
                    String idToken = result.idToken();
                    Integer expiresIn = result.expiresIn();
                    String tokenType = result.tokenType() != null ? result.tokenType() : "Bearer";

                    UserProfileDto profile = extractUserProfile(idToken != null ? idToken : accessToken, null);

                    return new LoginResponseDto(
                            accessToken,
                            idToken,
                            refreshToken,
                            expiresIn,
                            tokenType,
                            profile
                    );
                })
                .onErrorMap(err -> {
                    Throwable cause = err;
                    while (cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException) {
                        if (cause.getCause() != null) {
                            cause = cause.getCause();
                        } else {
                            break;
                        }
                    }
                    if (cause instanceof ResponseStatusException) {
                        return cause;
                    }
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token refresh failed: " + cause.getMessage());
                });
    }

    private String calculateSecretHash(String userName) {
        if (clientSecret == null || clientSecret.isBlank()) {
            return null;
        }
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
        SecretKeySpec signingKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            log.error("Failed to calculate SECRET_HASH", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate authentication hash");
        }
    }

    private UserProfileDto extractUserProfile(String jwtToken, String fallbackEmail) {
        if (jwtToken == null || !jwtToken.contains(".")) {
            return new UserProfileDto("unknown", fallbackEmail, Collections.emptyList());
        }

        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length >= 2) {
                byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
                String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                JsonNode payload = objectMapper.readTree(payloadJson);

                String sub = payload.has("sub") ? payload.get("sub").asText() : "unknown";
                String email = payload.has("email") ? payload.get("email").asText() : fallbackEmail;

                List<String> roles = new ArrayList<>();
                if (payload.has("cognito:groups")) {
                    JsonNode groupsNode = payload.get("cognito:groups");
                    if (groupsNode.isArray()) {
                        for (JsonNode group : groupsNode) {
                            roles.add(group.asText().toUpperCase());
                        }
                    }
                }
                if (roles.isEmpty()) {
                    roles.add("BUYER");
                }

                return new UserProfileDto(sub, email, roles);
            }
        } catch (Exception e) {
            log.warn("Failed to parse JWT claims for user profile: {}", e.getMessage());
        }

        return new UserProfileDto("unknown", fallbackEmail, List.of("BUYER"));
    }
}
