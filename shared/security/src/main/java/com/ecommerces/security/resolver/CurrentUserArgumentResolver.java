package com.ecommerces.security.resolver;

import com.ecommerces.security.annotation.CurrentUser;
import com.ecommerces.security.context.UserContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring MVC Argument Resolver that extracts Cognito JWT claims from {@link SecurityContextHolder}
 * and constructs a {@link UserContext} instance for controller parameters annotated with {@link CurrentUser}.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                UserContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = annotation == null || annotation.required();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
            Jwt jwt = jwtAuthToken.getToken();

            String userId = jwt.getClaimAsString("sub");
            if (userId == null || userId.isBlank()) {
                userId = jwt.getClaimAsString("username");
            }
            if (userId == null || userId.isBlank()) {
                userId = jwtAuthToken.getName();
            }

            String email = jwt.getClaimAsString("email");

            List<String> rawGroups = jwt.getClaimAsStringList("cognito:groups");
            List<String> roles = new ArrayList<>();
            if (rawGroups != null && !rawGroups.isEmpty()) {
                for (String g : rawGroups) {
                    roles.add(g.toUpperCase());
                }
            } else {
                roles.add("BUYER");
            }

            return new UserContext(userId, email, roles);
        }

        if (required) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required: Missing or invalid JWT Bearer token");
        }

        return null;
    }
}
