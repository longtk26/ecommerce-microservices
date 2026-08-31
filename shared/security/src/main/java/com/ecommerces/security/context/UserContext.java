package com.ecommerces.security.context;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates authenticated user context resolved from JWT claims.
 *
 * @param userId Unique user ID from Cognito 'sub' claim
 * @param email  User email address
 * @param roles  Assigned user roles (e.g., BUYER, SELLER, ADMIN)
 */
public record UserContext(
        String userId,
        String email,
        List<String> roles
) {
    public UserContext {
        roles = (roles != null) ? List.copyOf(roles) : Collections.emptyList();
    }

    public boolean hasRole(String role) {
        if (role == null || roles == null) {
            return false;
        }
        String normalizedRole = role.toUpperCase();
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring(5);
        }
        for (String r : roles) {
            String nr = r.toUpperCase();
            if (nr.startsWith("ROLE_")) {
                nr = nr.substring(5);
            }
            if (nr.equals(normalizedRole)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBuyer() {
        return hasRole("BUYER");
    }

    public boolean isSeller() {
        return hasRole("SELLER");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
