package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;

/**
 * Разрешитель {@code CapabilityAdmissionActorResolver}.
 */
@Deprecated(forRemoval = true)
public class CapabilityAdmissionActorResolver extends InteractiveActorResolver {

    public CapabilityAdmissionActorResolver(AuthenticatedActorAdapter authenticatedActorAdapter) {
        super(authenticatedActorAdapter);
    }
}
