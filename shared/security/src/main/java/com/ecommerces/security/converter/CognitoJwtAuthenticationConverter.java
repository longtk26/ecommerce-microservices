package com.ecommerces.security.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts Cognito JWT 'cognito:groups' claims into Spring Security 'ROLE_<GROUP>' granted authorities.
 */
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter delegate;

    public CognitoJwtAuthenticationConverter() {
        this.delegate = new JwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null || groups.isEmpty()) {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"));
            }
            return groups.stream()
                    .map(group -> {
                        String upper = group.toUpperCase();
                        return (GrantedAuthority) new SimpleGrantedAuthority(upper.startsWith("ROLE_") ? upper : "ROLE_" + upper);
                    })
                    .collect(Collectors.toList());
        });
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return delegate.convert(jwt);
    }
}
