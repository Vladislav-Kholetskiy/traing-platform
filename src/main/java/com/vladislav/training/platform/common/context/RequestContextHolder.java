package com.vladislav.training.platform.common.context;

import java.util.Objects;
import java.util.Optional;

/**
 * Класс {@code RequestContextHolder}.
 */
public final class RequestContextHolder {

    private static final ThreadLocal<CurrentRequestContext> CURRENT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(CurrentRequestContext context) {
        CURRENT.set(Objects.requireNonNull(context, "context must not be null"));
    }

    public static Optional<CurrentRequestContext> getCurrent() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static CurrentRequestContext getRequired() {
        return getCurrent().orElseThrow(
            () -> new IllegalStateException("Request context is not initialized")
        );
    }

    public static void clear() {
        CURRENT.remove();
    }
}
