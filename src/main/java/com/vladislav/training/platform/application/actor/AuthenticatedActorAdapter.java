package com.vladislav.training.platform.application.actor;

import java.security.Principal;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Класс {@code AuthenticatedActorAdapter}.
 */
@Component
public class AuthenticatedActorAdapter {

    public Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    public ResolvedAuthenticatedActor requireResolvedInteractiveActor() {
        Authentication authentication = currentAuthentication();
        Objects.requireNonNull(authentication, "authentication must not be null");
        if (!isAuthenticated(authentication)) {
            throw new IllegalStateException("Authenticated principal is required");
        }
        return requireResolvedInteractiveActor(authentication);
    }

    public ResolvedAuthenticatedActor requireResolvedInteractiveActor(Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication must not be null");
        if (!isAuthenticated(authentication)) {
            throw new IllegalStateException("Authenticated principal is required");
        }
        Long actorUserId = resolveActorUserId(authentication);
        return new ResolvedAuthenticatedActor(actorUserId, resolvePrincipalName(authentication), authentication);
    }

    public Long resolveActorUserId(Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication must not be null");

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedActorPrincipal actorPrincipal) {
            return Objects.requireNonNull(actorPrincipal.actorUserId(), "actorUserId must not be null");
        }
        if (principal instanceof Number numberPrincipal) {
            return numberPrincipal.longValue();
        }
        if (principal instanceof Principal namedPrincipal) {
            Long parsed = tryParseLong(namedPrincipal.getName());
            if (parsed != null) {
                return parsed;
            }
        }
        Long parsedFromAuthenticationName = tryParseLong(authentication.getName());
        if (parsedFromAuthenticationName != null) {
            return parsedFromAuthenticationName;
        }
        throw new IllegalStateException("Authenticated principal cannot be resolved to actorUserId");
    }

    private String resolvePrincipalName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Principal namedPrincipal) {
            return namedPrincipal.getName();
        }
        String authenticationName = authentication.getName();
        return authenticationName != null ? authenticationName : String.valueOf(principal);
    }

    private Long tryParseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
