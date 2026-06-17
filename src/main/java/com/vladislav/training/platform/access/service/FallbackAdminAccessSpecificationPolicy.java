package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.application.actor.ResolvedAuthenticatedActor;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.security.core.Authentication;

/**
 * Класс {@code FallbackAdminAccessSpecificationPolicy}.
 */
public class FallbackAdminAccessSpecificationPolicy implements AccessSpecificationPolicy {

    private final InteractiveActorResolver interactiveActorResolver;

    public FallbackAdminAccessSpecificationPolicy(InteractiveActorResolver interactiveActorResolver) {
        this.interactiveActorResolver = interactiveActorResolver;
    }

    @Override
    public AccessReadScope resolveReadScope(AccessPolicyQueryContext context) {
        Authentication authentication = validateReadRequest(context.actorUserId(), context.effectiveAt());
        return hasAdminAuthority(authentication) ? AccessReadScope.fullAccess() : AccessReadScope.denyAll();
    }

    @Override
    public boolean canReadUserAdministrationData(Long userId, Instant effectiveAt) {
        Authentication authentication = validateReadRequest(userId, effectiveAt);
        return hasAdminAuthority(authentication);
    }

    @Override
    public boolean canReadAccessManagementData(Long userId, Instant effectiveAt) {
        return canReadUserAdministrationData(userId, effectiveAt);
    }

    private Authentication validateReadRequest(Long userId, Instant effectiveAt) {
        if (userId == null) {
            throw new PolicyViolationException("AccessSpecificationPolicy requires non-null actorUserId");
        }
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");

        ResolvedAuthenticatedActor resolvedActor = interactiveActorResolver.resolveActor();
        if (!Objects.equals(userId, resolvedActor.actorUserId())) {
            throw new PolicyViolationException(
                "AccessSpecificationPolicy actorUserId does not match the current authenticated principal"
            );
        }
        return resolvedActor.authentication();
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> "ROLE_ADMIN".equals(grantedAuthority.getAuthority()));
    }
}

