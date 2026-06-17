package com.vladislav.training.platform.application.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.common.exception.PolicyViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code InteractiveActorResolver}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class InteractiveActorResolverTest {

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveActorUsesCanonicalInteractiveActorBridge() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(101L, null, "ROLE_ADMIN"));

        ResolvedAuthenticatedActor resolvedActor = interactiveActorResolver.resolveActor();

        assertThat(resolvedActor.actorUserId()).isEqualTo(101L);
        assertThat(resolvedActor.authentication().getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void resolveActorFailsClosedWhenPrincipalCannotBeResolved() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("admin@example.local", null, "ROLE_ADMIN")
        );

        assertThatThrownBy(() -> interactiveActorResolver.resolveActor())
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("canonical runtime actor bridge");
    }
}
