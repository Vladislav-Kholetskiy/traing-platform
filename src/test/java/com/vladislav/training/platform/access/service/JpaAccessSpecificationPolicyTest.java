package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.io.IOException;
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
 * Проверяет поведение {@code JpaAccessSpecificationPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class JpaAccessSpecificationPolicyTest {

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T12:00:00Z");

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveReadScopeReturnsFullAccessForAdminAuthorityWithoutAreaRestriction() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.USER_ADMINISTRATION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "app_user"
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        assertThat(scope.unitOnlyIds()).isEmpty();
        assertThat(scope.subtreePaths()).isEmpty();

        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeReturnsFullAccessForAdminRoleAssignmentWithoutAreaRestriction() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            null,
            null,
            "user_access_area"
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsNonAdminFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );
        when(accessFoundationStateReadService.findActiveTemporaryRoleIds(101L, FIXED_INSTANT)).thenReturn(Set.of(502L));
        when(userOrgFoundationStateReadService.findRoleCodesByIds(Set.of(502L))).thenReturn(Set.of("SUPPORT"));

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.USER_ADMINISTRATION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "app_user"
        ));

        assertThat(scope.readAllowed()).isFalse();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
    }

    @Test
    void resolveReadScopeRejectsInactiveActorBeforeAccessMaterialization() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, false, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            null,
            null,
            "user_access_area"
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeRejectsMismatchedActorContext() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));

        assertThatThrownBy(() -> policy.resolveReadScope(new AccessPolicyQueryContext(
            999L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.TREE,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit"
        ))).isInstanceOf(PolicyViolationException.class);

        verifyNoInteractions(userOrgFoundationStateReadService);
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeReturnsFullAccessForSystemAdminAuthorityWithoutAreaRestriction() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_SYSTEM_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.TREE,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit"
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeAllowsAssignmentCampaignPreviewContourForExpertAuthorityOnKnownInternalFamily() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign_preview"
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeAllowsAssignmentAssignmentListContourForExpertAuthorityOnCanonicalFamily() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsAssignmentAssignmentDetailContourAllowedForCanonicalFamily() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeAllowsAssignedLearningContextDetailForSelfScopedAssignmentContour() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("OPERATOR"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_learning_context",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsAssignedLearningContextWithoutLearnerAuthorityFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("SUPPORT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assigned_learning_context",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
    }

    @Test
    void resolveReadScopeAllowsAssignedCurrentAttemptDetailForTestingSpecificSelfScopedContour() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_OPERATOR"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("OPERATOR"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_current_attempt",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsAssignedCurrentAttemptDetailWithoutLearnerAuthorityFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("SUPPORT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_current_attempt",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
    }

    @Test
    void resolveReadScopeAllowsSelfCurrentAttemptDetailForActiveActorSelfFoundationStateWithoutTemporaryRoleLookup() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            501L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfCurrentAttemptMissingTargetTestIdFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            null,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeAllowsSelfVisibleTestingListForActiveActorSelfContext() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeAllowsSelfVisibleTestingDetailForActiveActorSelfContext() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfVisibleTestingWrongTargetFamilyFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "test",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfVisibleTestingWrongSubjectScopeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfVisibleTestingWrongSubjectSemanticsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.UNSPECIFIED
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfVisibleTestingUnsupportedReadTypeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_VISIBLE_TESTING,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsContentAuthoringFromActingAsSelfVisibleTestingAlias() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.CONTENT_AUTHORING,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_visible_testing",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void selfVisibleTestingContourExistsAndIsDistinctFromContentAuthoring() {
        assertThat(AccessReadArea.valueOf("SELF_VISIBLE_TESTING")).isEqualTo(AccessReadArea.SELF_VISIBLE_TESTING);
        assertThat(AccessReadArea.SELF_VISIBLE_TESTING).isNotEqualTo(AccessReadArea.CONTENT_AUTHORING);
    }

    @Test
    void resolveReadScopeKeepsSelfCurrentAttemptWrongTargetFamilyFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            501L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfCurrentAttemptWrongReadTypeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            501L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfCurrentAttemptWrongSubjectScopeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            501L,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfCurrentAttemptWrongSubjectSemanticsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            501L,
            null,
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.UNSPECIFIED
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsUnspecifiedAssignmentSubjectScopeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment"
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsUnsupportedSelfScopedAssignmentCombinationsFailClosedEvenForExpertAuthority() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of())
        );

        AccessReadScope assignmentTestScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment_test",
            AccessReadSubjectScope.ACTOR_SELF
        ));
        AccessReadScope previewScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign_preview",
            AccessReadSubjectScope.ACTOR_SELF
        ));
        AccessReadScope auditScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF
        ));
        AccessReadScope reportingScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT_CAMPAIGN,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment_campaign",
            AccessReadSubjectScope.ACTOR_SELF
        ));
        AccessReadScope assignedLearningListScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assigned_learning_context",
            AccessReadSubjectScope.ACTOR_SELF
        ));
        AccessReadScope assignedCurrentAttemptUnspecifiedScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assigned_current_attempt"
        ));
        AccessReadScope assignedCurrentAttemptWrongFamilyScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNED_CURRENT_ATTEMPT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF
        ));

        assertThat(assignmentTestScope.readAllowed()).isFalse();
        assertThat(previewScope.readAllowed()).isFalse();
        assertThat(auditScope.readAllowed()).isFalse();
        assertThat(reportingScope.readAllowed()).isFalse();
        assertThat(assignedLearningListScope.readAllowed()).isFalse();
        assertThat(assignedCurrentAttemptUnspecifiedScope.readAllowed()).isFalse();
        assertThat(assignedCurrentAttemptWrongFamilyScope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeMaterializesAllCanonicalAnalyticsContoursWithoutCrossContourPrivilegeBleed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN", "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN", "EXPERT"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(30L), Set.of("/dept/30"))
        );

        AccessReadScope managerialCurrentSupervisionScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));
        AccessReadScope managerialHistoricalAnalyticsScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));
        AccessReadScope expertQuestionAnalyticsScope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));

        assertThat(managerialCurrentSupervisionScope.readAllowed()).isTrue();
        assertThat(managerialCurrentSupervisionScope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(managerialCurrentSupervisionScope.unitOnlyIds()).containsExactly(30L);
        assertThat(managerialCurrentSupervisionScope.subtreePaths()).containsExactly("/dept/30");
        assertThat(managerialHistoricalAnalyticsScope.readAllowed()).isTrue();
        assertThat(managerialHistoricalAnalyticsScope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(managerialHistoricalAnalyticsScope.unitOnlyIds()).containsExactly(30L);
        assertThat(managerialHistoricalAnalyticsScope.subtreePaths()).containsExactly("/dept/30");
        assertThat(expertQuestionAnalyticsScope.readAllowed()).isTrue();
        assertThat(expertQuestionAnalyticsScope.fullOrganizationalUnitAccess()).isTrue();
        assertThat(expertQuestionAnalyticsScope.unitOnlyIds()).isEmpty();
        assertThat(expertQuestionAnalyticsScope.subtreePaths()).isEmpty();
    }

    @Test
    void resolveReadScopeAllowsCanonicalSelfResultHistoryContextAfterResultRootedSeamClosure() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfResultHistoryWrongTargetFamilyFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_current_attempt",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfResultHistoryWrongReadTypeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfResultHistoryWrongSubjectSemanticsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.UNSPECIFIED
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsSelfResultHistoryWrongSubjectScopeFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "self_result_history",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.SELF
        ));

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeRejectsNullContextFailClosedForAnalyticsPolicyActivation() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN", "ROLE_EXPERT"));

        AccessReadScope scope = policy.resolveReadScope(null);

        assertThat(scope.readAllowed()).isFalse();
        verifyNoInteractions(userOrgFoundationStateReadService);
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeRequiresCanonicalManagerialCurrentSupervisionContextBeforeFutureActivation() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(30L), Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(scope.unitOnlyIds()).containsExactly(30L);
        assertThat(scope.subtreePaths()).isEmpty();
    }

    @Test
    void resolveReadScopeBuildsManagerialCurrentSupervisionSubtreeOnlyFromConfirmedOwnerFacts() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(30L), Set.of("/dept/30"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(scope.unitOnlyIds()).containsExactly(30L);
        assertThat(scope.subtreePaths()).containsExactly("/dept/30");
    }

    @Test
    void resolveReadScopeKeepsManagerialCurrentSupervisionWithoutManagementFactsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(), Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(scope.readAllowed()).isFalse();
    }

    @Test
    void resolveReadScopeKeepsManagerialCurrentSupervisionWrongFamilyTypeAndSemanticsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );

        AccessReadScope wrongFamily = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));
        AccessReadScope wrongType = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));
        AccessReadScope wrongSemantics = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));

        assertThat(wrongFamily.readAllowed()).isFalse();
        assertThat(wrongType.readAllowed()).isFalse();
        assertThat(wrongSemantics.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeRequiresCanonicalManagerialHistoricalAnalyticsContextBeforeFutureActivation() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(30L), Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(scope.unitOnlyIds()).containsExactly(30L);
        assertThat(scope.subtreePaths()).isEmpty();
    }

    @Test
    void resolveReadScopeBuildsManagerialHistoricalAnalyticsSubtreeOnlyFromConfirmedOwnerFacts() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(30L), Set.of("/dept/30"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
        assertThat(scope.unitOnlyIds()).containsExactly(30L);
        assertThat(scope.subtreePaths()).containsExactly("/dept/30");
    }

    @Test
    void resolveReadScopeKeepsManagerialHistoricalAnalyticsWithoutManagementFactsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );
        when(accessFoundationStateReadService.findActorManagerialScope(101L, FIXED_INSTANT)).thenReturn(
            new AccessFoundationStateReadService.ManagerialScopeFoundationState(Set.of(), Set.of())
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(scope.readAllowed()).isFalse();
    }

    @Test
    void resolveReadScopeKeepsManagerialHistoricalAnalyticsWrongFamilyTypeAndSemanticsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("ADMIN"))
        );

        AccessReadScope wrongFamily = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_current_supervision",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));
        AccessReadScope wrongType = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));
        AccessReadScope wrongSemantics = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));

        assertThat(wrongFamily.readAllowed()).isFalse();
        assertThat(wrongType.readAllowed()).isFalse();
        assertThat(wrongSemantics.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeRequiresCanonicalExpertQuestionAnalyticsContextBeforeFutureActivation() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));

        assertThat(scope.readAllowed()).isTrue();
        assertThat(scope.fullOrganizationalUnitAccess()).isTrue();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void resolveReadScopeKeepsExpertQuestionAnalyticsWithoutExplicitExpertFactsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("SUPPORT"))
        );
        when(accessFoundationStateReadService.findActiveTemporaryRoleIds(101L, FIXED_INSTANT)).thenReturn(Set.of(700L));
        when(userOrgFoundationStateReadService.findRoleCodesByIds(Set.of(700L))).thenReturn(Set.of("SUPPORT"));

        AccessReadScope scope = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));

        assertThat(scope.readAllowed()).isFalse();
        assertThat(scope.fullOrganizationalUnitAccess()).isFalse();
    }

    @Test
    void resolveReadScopeKeepsExpertQuestionAnalyticsWrongFamilyTypeAndSemanticsFailClosed() {
        JpaAccessSpecificationPolicy policy = new JpaAccessSpecificationPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        setAuthentication(authenticatedToken(101L, "ROLE_EXPERT"));
        when(userOrgFoundationStateReadService.findUserAccessPolicyFoundationState(101L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.UserAccessPolicyFoundationState(101L, true, Set.of("EXPERT"))
        );

        AccessReadScope wrongFamily = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "managerial_historical_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));
        AccessReadScope wrongType = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        ));
        AccessReadScope wrongSemantics = policy.resolveReadScope(new AccessPolicyQueryContext(
            101L,
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            FIXED_INSTANT,
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.MANAGER
        ));

        assertThat(wrongFamily.readAllowed()).isFalse();
        assertThat(wrongType.readAllowed()).isFalse();
        assertThat(wrongSemantics.readAllowed()).isFalse();
        verifyNoInteractions(accessFoundationStateReadService);
    }

    @Test
    void analyticsPolicyActivationDoesNotDependOnAdmissionCommandOrAnalyticsMutationCollaborators() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
        ));

        assertThat(source)
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentAdministrativeActionService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("refresh(")
            .doesNotContain("rebuild(")
            .doesNotContain("recalculate(")
            .doesNotContain("triggerRecovery")
            .contains("canReadManagerialCurrentSupervision")
            .contains("canReadManagerialHistoricalAnalytics")
            .contains("canReadExpertQuestionAnalytics");
    }

    private void setAuthentication(TestingAuthenticationToken authentication) {
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private TestingAuthenticationToken authenticatedToken(Long userId, String... authorities) {
        return new TestingAuthenticationToken(userId, null, authorities);
    }
}
