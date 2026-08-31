package com.ecommerces.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter-level annotation to inject the resolved {@link com.ecommerces.security.context.UserContext}
 * directly into Controller methods from the current Spring Security context.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /**
     * Whether an authenticated user is required.
     * If true and no valid JWT is present, an HTTP 401 Unauthorized exception is thrown.
     * If false and unauthenticated, null is injected.
     */
    boolean required() default true;
}
