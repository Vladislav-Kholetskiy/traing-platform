package com.vladislav.training.platform.application.actor;

import com.vladislav.training.platform.application.policy.CapabilityViolationCode;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Разрешитель {@code InteractiveActorResolver}.
 */
@Component
public class InteractiveActorResolver {

    private final AuthenticatedActorAdapter authenticatedActorAdapter;

    public InteractiveActorResolver(AuthenticatedActorAdapter authenticatedActorAdapter) {
        this.authenticatedActorAdapter = authenticatedActorAdapter;
    }

    public ResolvedAuthenticatedActor resolveActor() {
        Authentication authentication = authenticatedActorAdapter.currentAuthentication();
        if (!authenticatedActorAdapter.isAuthenticated(authentication)) {
            throw new PolicyViolationException(
                CapabilityViolationCode.ACTOR_UNAUTHENTICATED.name(),
                "Authenticated principal is required for interactive actor resolution"
            );
        }
        try {
            return authenticatedActorAdapter.requireResolvedInteractiveActor(authentication);
        } catch (IllegalStateException exception) {
            throw new PolicyViolationException(
                CapabilityViolationCode.ACTOR_CONTEXT_MISMATCH.name(),
                "Authenticated principal cannot be resolved to actorUserId through the canonical runtime actor bridge"
            );
        }
    }

    public Long resolveActorUserId() {
        return resolveActor().actorUserId();
    }
}
