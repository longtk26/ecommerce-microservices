package com.ecommerces.security;

import com.ecommerces.security.annotation.CurrentUser;
import com.ecommerces.security.context.UserContext;
import com.ecommerces.security.converter.CognitoJwtAuthenticationConverter;
import com.ecommerces.security.resolver.CurrentUserArgumentResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityComponentsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void sampleControllerMethod(@CurrentUser UserContext user,
                                        @CurrentUser(required = false) UserContext optionalUser,
                                        String plainParam) {
    }

    @Test
    void testUserContextRoleChecks() {
        UserContext buyer = new UserContext("user-1", "buyer@test.com", List.of("BUYER"));
        assertTrue(buyer.isBuyer());
        assertFalse(buyer.isSeller());
        assertFalse(buyer.isAdmin());
        assertTrue(buyer.hasRole("ROLE_BUYER"));

        UserContext seller = new UserContext("user-2", "seller@test.com", List.of("SELLER"));
        assertTrue(seller.isSeller());
        assertFalse(seller.isBuyer());
    }

    @Test
    void testCognitoJwtAuthenticationConverter() {
        CognitoJwtAuthenticationConverter converter = new CognitoJwtAuthenticationConverter();
        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-123",
                        "email", "user@test.com",
                        "cognito:groups", List.of("seller", "admin")
                )
        );

        var token = converter.convert(jwt);
        assertNotNull(token);
        List<String> auths = token.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(auths.contains("ROLE_SELLER"));
        assertTrue(auths.contains("ROLE_ADMIN"));
    }

    @Test
    void testCurrentUserArgumentResolverSuccess() throws Exception {
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
        Method method = getClass().getDeclaredMethod("sampleControllerMethod", UserContext.class, UserContext.class, String.class);

        MethodParameter requiredParam = new MethodParameter(method, 0);
        MethodParameter plainParam = new MethodParameter(method, 2);

        assertTrue(resolver.supportsParameter(requiredParam));
        assertFalse(resolver.supportsParameter(plainParam));

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "user-456",
                        "email", "alice@test.com",
                        "cognito:groups", List.of("BUYER")
                )
        );
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Object resolved = resolver.resolveArgument(requiredParam, null, null, null);
        assertNotNull(resolved);
        assertInstanceOf(UserContext.class, resolved);

        UserContext user = (UserContext) resolved;
        assertEquals("user-456", user.userId());
        assertEquals("alice@test.com", user.email());
        assertTrue(user.isBuyer());
    }

    @Test
    void testCurrentUserArgumentResolverUnauthenticatedThrows() throws Exception {
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
        Method method = getClass().getDeclaredMethod("sampleControllerMethod", UserContext.class, UserContext.class, String.class);

        MethodParameter requiredParam = new MethodParameter(method, 0);
        MethodParameter optionalParam = new MethodParameter(method, 1);

        assertThrows(ResponseStatusException.class, () -> resolver.resolveArgument(requiredParam, null, null, null));

        Object resolvedOptional = resolver.resolveArgument(optionalParam, null, null, null);
        assertNull(resolvedOptional);
    }
}
