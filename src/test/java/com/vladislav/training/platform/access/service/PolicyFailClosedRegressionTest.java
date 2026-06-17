package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет, что {@code PolicyFailClosed} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
@ExtendWith(MockitoExtension.class)
class PolicyFailClosedRegressionTest {

    private static final Long UNAUTHORIZED_ACTOR_ID = 202L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T22:30:00Z");

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private com.vladislav.training.platform.application.actor.InteractiveActorResolver queryActorResolver;
    @Mock
    private UtcClock utcClock;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthorizedActorRemainsFailClosedForAdministrativeReadContoursEvenAfterNarrowAllowExists() throws Exception {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        when(queryActorResolver.resolveActorUserId()).thenReturn(UNAUTHORIZED_ACTOR_ID);
        when(utcClock.now()).thenReturn(FIXED_INSTANT);
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(UNAUTHORIZED_ACTOR_ID, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(UNAUTHORIZED_ACTOR_ID, true, Set.of("SUPPORT"))
        );
        setAuthentication(authenticatedToken(UNAUTHORIZED_ACTOR_ID));

        AccessPolicyQueryContextResolver resolver = new AccessPolicyQueryContextResolver(queryActorResolver, utcClock);

        assertDenied(policy.resolveReadScope(resolver.resolveNotificationAdministrationContext(UNAUTHORIZED_ACTOR_ID)));
        assertDenied(policy.resolveReadScope(resolver.resolveNotificationAdministrationDetailContext(UNAUTHORIZED_ACTOR_ID, 701L)));
        assertDenied(policy.resolveReadScope(resolver.resolveNotificationRuleAdministrationContext(UNAUTHORIZED_ACTOR_ID)));
        assertDenied(policy.resolveReadScope(resolver.resolveNotificationRuleAdministrationDetailContext(UNAUTHORIZED_ACTOR_ID, 702L)));
        assertDenied(policy.resolveReadScope(resolver.resolveImportJobAdministrationContext(UNAUTHORIZED_ACTOR_ID)));
        assertDenied(policy.resolveReadScope(resolver.resolveImportJobAdministrationDetailContext(UNAUTHORIZED_ACTOR_ID, 703L)));
        assertDenied(policy.resolveReadScope(resolver.resolveImportJobItemAdministrationContext(UNAUTHORIZED_ACTOR_ID)));
        assertDenied(policy.resolveReadScope(resolver.resolveImportJobItemAdministrationDetailContext(UNAUTHORIZED_ACTOR_ID, 704L)));
        assertDenied(policy.resolveReadScope(resolver.resolveAuditEventAdministrationContext(UNAUTHORIZED_ACTOR_ID)));
        assertDenied(policy.resolveReadScope(resolver.resolveAuditEventAdministrationDetailContext(UNAUTHORIZED_ACTOR_ID, 705L)));
    }

    @Test
    void jpaPolicySourceShowsNoBroadAdministrativeOrCommandMutationDrift() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
        ));

        assertThat(source)
            .doesNotContain("ADMIN_ALL")
            .doesNotContain("GENERIC_ADMIN")
            .doesNotContain("OPERATIONAL_ADMINISTRATION")
            .doesNotContain("TABLE_ADMINISTRATION")
            .doesNotContain("CapabilityAdmissionRequestFactory")
            .doesNotContain("CapabilityOperationCode")
            .doesNotContain("CapabilityTargetEntityType")
            .doesNotContain("owner_table")
            .doesNotContain("database_table")
            .doesNotContain("audit_event_mutation")
            .doesNotContain("fullAccessForAdministrative")
            .doesNotContain("allowAllAdministrative")
            .doesNotContain("permitAllAdministrative");
    }

    private void assertDenied(AccessReadScope scope) {
        assertThat(scope.readAllowed()).isFalse();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(scope.unitOnlyIds()).isEmpty();
        assertThat(scope.subtreePaths()).isEmpty();
    }

    private void setAuthentication(TestingAuthenticationToken authentication) {
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private TestingAuthenticationToken authenticatedToken(Long userId, String... authorities) {
        return new TestingAuthenticationToken(userId, null, authorities);
    }
}
