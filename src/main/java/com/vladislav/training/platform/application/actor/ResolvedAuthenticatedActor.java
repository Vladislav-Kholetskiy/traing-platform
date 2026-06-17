package com.vladislav.training.platform.application.actor;

import java.util.Objects;
import org.springframework.security.core.Authentication;

/**
 * Запись данных {@code ResolvedAuthenticatedActor}.
 */
public record ResolvedAuthenticatedActor(
    Long actorUserId,
    String principalName,
    Authentication authentication
) {

    public ResolvedAuthenticatedActor {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(principalName, "principalName must not be null");
        Objects.requireNonNull(authentication, "authentication must not be null");
    }
}
