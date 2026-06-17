package com.vladislav.training.platform.application.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
/**
 * Проверяет поведение {@code AuthenticatedActorAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AuthenticatedActorAdapterTest {

    private final AuthenticatedActorAdapter authenticatedActorAdapter = new AuthenticatedActorAdapter();

    @Test
    void resolvesActorUserIdFromRuntimePrincipalContractWithoutNumericAuthenticationName() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            new TestActorPrincipal(42L, "admin@example.local"),
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );

        ResolvedAuthenticatedActor resolvedActor = authenticatedActorAdapter.requireResolvedInteractiveActor(authentication);

        assertThat(resolvedActor.actorUserId()).isEqualTo(42L);
        assertThat(resolvedActor.principalName()).isEqualTo("admin@example.local");
    }

    @Test
    void stillSupportsLegacyNumericPrincipalAsTransitionalCompatibilityPath() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            99L,
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );

        ResolvedAuthenticatedActor resolvedActor = authenticatedActorAdapter.requireResolvedInteractiveActor(authentication);

        assertThat(resolvedActor.actorUserId()).isEqualTo(99L);
    }

    @Test
    void rejectsUnresolvablePrincipalShape() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "admin@example.local",
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );

        assertThatThrownBy(() -> authenticatedActorAdapter.requireResolvedInteractiveActor(authentication))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be resolved");
    }

    private record TestActorPrincipal(Long actorUserId, String principalName) implements AuthenticatedActorPrincipal {
        @Override
        public String toString() {
            return principalName;
        }
    }
}
