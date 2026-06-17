package com.vladislav.training.platform.application.actor.dev;

import com.vladislav.training.platform.application.actor.AuthenticatedActorPrincipal;
import java.security.Principal;

/**
 * Запись данных {@code DevDemoActorPrincipal}.
 */
record DevDemoActorPrincipal(
    Long actorUserId,
    String name
) implements AuthenticatedActorPrincipal, Principal {

    @Override
    public String getName() {
        return name;
    }
}
