package com.vladislav.training.platform.application.policy;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;

import com.vladislav.training.platform.application.actor.ResolvedAuthenticatedActor;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.Objects;
import org.springframework.security.core.Authentication;

/**
 * Класс {@code FallbackAdminCapabilityAdmissionPolicy}.
 */
public class FallbackAdminCapabilityAdmissionPolicy implements CapabilityAdmissionPolicy {

    private final InteractiveActorResolver interactiveActorResolver;

    public FallbackAdminCapabilityAdmissionPolicy(InteractiveActorResolver interactiveActorResolver) {
        this.interactiveActorResolver = interactiveActorResolver;
    }

    @Override
    public void check(CapabilityAdmissionRequest request) {
        ResolvedAuthenticatedActor resolvedActor = interactiveActorResolver.resolveActor();
        Authentication authentication = resolvedActor.authentication();
        if (!Objects.equals(request.actorUserId(), resolvedActor.actorUserId())) {
            throw new PolicyViolationException(
                CapabilityViolationCode.ACTOR_CONTEXT_MISMATCH.name(),
                "Capability admission actorUserId does not match the current authenticated principal"
            );
        }
        if (!hasAdminAuthority(authentication)) {
            throw new PolicyViolationException(
                CapabilityViolationCode.ACTOR_NOT_AUTHORIZED.name(),
                "Capability admission is temporarily restricted to ROLE_ADMIN actors in foundation contour"
            );
        }
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> "ROLE_ADMIN".equals(grantedAuthority.getAuthority()));
    }
}

