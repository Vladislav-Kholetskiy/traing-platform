package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет, что {@code CommandAdmissionNarrowAllow} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
@ExtendWith(MockitoExtension.class)
class CommandAdmissionNarrowAllowRegressionTest {

    private static final Long ADMIN_ACTOR_ID = 101L;
    private static final Long UNAUTHORIZED_ACTOR_ID = 202L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T21:00:00Z");

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private SystemActorResolver systemActorResolver;

    private DefaultCapabilityAdmissionPolicy policy;
    private CapabilityAdmissionRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        policy = new DefaultCapabilityAdmissionPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        policy.setSystemActorResolver(systemActorResolver);
        requestFactory = new CapabilityAdmissionRequestFactory(interactiveActorResolver, fixedClock());
        lenient().when(userOrgFoundationStateReadService.findActorCommandFoundationState(ADMIN_ACTOR_ID, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(ADMIN_ACTOR_ID, true, Set.of())
        );
        lenient().when(userOrgFoundationStateReadService.findActorCommandFoundationState(UNAUTHORIZED_ACTOR_ID, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(UNAUTHORIZED_ACTOR_ID, true, Set.of("SUPPORT"))
        );
        lenient().when(accessFoundationStateReadService.findActiveTemporaryRoleIds(UNAUTHORIZED_ACTOR_ID, FIXED_INSTANT))
            .thenReturn(Set.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorizedAdministrativeActorShouldGainAdministrativeCommandAdmissionWithoutGenericShortcuts() {
        setAuthentication(authenticatedToken(ADMIN_ACTOR_ID, "ROLE_ADMIN"));

        List<CapabilityAdmissionRequest> requests = List.of(
            requestFactory.createNotificationRuleCreate(),
            requestFactory.createNotificationRuleUpdate(701L),
            requestFactory.createNotificationRuleEnable(702L),
            requestFactory.createNotificationRuleDisable(703L),
            requestFactory.createImportJobLaunch(),
            requestFactory.createImportItemReviewApply(801L),
            requestFactory.createImportItemReviewReject(802L)
        );

        assertThat(requests)
            .extracting(CapabilityAdmissionRequest::operationCode)
            .containsExactly(
                CapabilityOperationCodes.NOTIFICATION_RULE_CREATE,
                CapabilityOperationCodes.NOTIFICATION_RULE_UPDATE,
                CapabilityOperationCodes.NOTIFICATION_RULE_ENABLE,
                CapabilityOperationCodes.NOTIFICATION_RULE_DISABLE,
                CapabilityOperationCodes.IMPORT_JOB_LAUNCH,
                CapabilityOperationCodes.IMPORT_ITEM_REVIEW_APPLY,
                CapabilityOperationCodes.IMPORT_ITEM_REVIEW_REJECT
            )
            .doesNotContain(
                "GENERIC_ADMIN_COMMAND",
                "OPERATIONAL_ADMIN_COMMAND",
                "TABLE_MUTATE",
                "OWNER_TABLE_PATCH",
                "IMPORT_OWNER_TABLE_PATCH",
                "AUDIT_EVENT_MUTATE",
                "AUDIT_EVENT_CREATE",
                "AUDIT_EVENT_UPDATE"
            );
        assertThat(requests)
            .extracting(request -> request.targetEntityType().name())
            .containsExactly(
                CapabilityTargetEntityType.NOTIFICATION_RULE.name(),
                CapabilityTargetEntityType.NOTIFICATION_RULE.name(),
                CapabilityTargetEntityType.NOTIFICATION_RULE.name(),
                CapabilityTargetEntityType.NOTIFICATION_RULE.name(),
                CapabilityTargetEntityType.IMPORT_JOB.name(),
                CapabilityTargetEntityType.IMPORT_JOB_ITEM.name(),
                CapabilityTargetEntityType.IMPORT_JOB_ITEM.name()
            )
            .doesNotContain(
                "GENERIC_ADMIN",
                "OPERATIONAL_TABLE",
                "DATABASE_TABLE",
                "OWNER_TABLE",
                "AUDIT_EVENT"
            );

        for (CapabilityAdmissionRequest request : requests) {
            assertThatCode(() -> policy.check(request)).doesNotThrowAnyException();
        }
    }

    @Test
    void unauthorizedActorRemainsDeniedForAdministrativeCommandAdmission() {
        setAuthentication(authenticatedToken(UNAUTHORIZED_ACTOR_ID, "ROLE_USER"));

        List<CapabilityAdmissionRequest> requests = List.of(
            requestFactory.createNotificationRuleCreate(),
            requestFactory.createNotificationRuleUpdate(701L),
            requestFactory.createNotificationRuleEnable(702L),
            requestFactory.createNotificationRuleDisable(703L),
            requestFactory.createImportJobLaunch(),
            requestFactory.createImportItemReviewApply(801L),
            requestFactory.createImportItemReviewReject(802L)
        );

        for (CapabilityAdmissionRequest request : requests) {
            assertThatThrownBy(() -> policy.check(request))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("not authorized");
        }
    }

    @Test
    void importReviewAdmissionRejectsWrongTargetType() {
        setAuthentication(authenticatedToken(ADMIN_ACTOR_ID, "ROLE_ADMIN"));
        CapabilityAdmissionRequest wrongTargetTypeRequest = new CapabilityAdmissionRequest(
            ADMIN_ACTOR_ID,
            CapabilityOperationCodes.IMPORT_ITEM_REVIEW_APPLY,
            CapabilityTargetEntityType.IMPORT_JOB,
            801L,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(wrongTargetTypeRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("IMPORT_JOB_ITEM target type");
    }

    @Test
    void importReviewAdmissionRejectsMissingTargetId() {
        setAuthentication(authenticatedToken(ADMIN_ACTOR_ID, "ROLE_ADMIN"));
        CapabilityAdmissionRequest missingTargetIdRequest = new CapabilityAdmissionRequest(
            ADMIN_ACTOR_ID,
            CapabilityOperationCodes.IMPORT_ITEM_REVIEW_REJECT,
            CapabilityTargetEntityType.IMPORT_JOB_ITEM,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(missingTargetIdRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("required target foundation fact");
    }

    @Test
    void canonicalCapabilityAdmissionPolicySourceShowsNoBroadAdministrativeCommandShortcuts() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/application/policy/DefaultCapabilityAdmissionPolicy.java"
        ));

        assertThat(source)
            .doesNotContain("allowAllAdministrativeCommands")
            .doesNotContain("permitAllAdministrativeCommands")
            .doesNotContain("fullCommandAccessForAdministrative")
            .doesNotContain("GENERIC_ADMIN_COMMAND")
            .doesNotContain("OWNER_TABLE_PATCH")
            .doesNotContain("IMPORT_OWNER_TABLE_PATCH")
            .doesNotContain("AUDIT_EVENT_MUTATE");
    }

    @Test
    void technicalSystemAdmissionBypassRemainsLimitedToImportJobLaunchOnly() {
        SecurityContextHolder.clearContext();
        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(9900L);
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(9900L, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(9900L, true, Set.of())
        );

        CapabilityAdmissionRequest importLaunchRequest = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCodes.IMPORT_JOB_LAUNCH,
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        CapabilityAdmissionRequest notificationRuleRequest = new CapabilityAdmissionRequest(
            9900L,
            CapabilityOperationCodes.NOTIFICATION_RULE_CREATE,
            CapabilityTargetEntityType.NOTIFICATION_RULE,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(importLaunchRequest)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.check(notificationRuleRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");
    }

    private UtcClock fixedClock() {
        return () -> FIXED_INSTANT;
    }

    private void setAuthentication(TestingAuthenticationToken authentication) {
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private TestingAuthenticationToken authenticatedToken(Long userId, String... authorities) {
        return new TestingAuthenticationToken(userId, null, authorities);
    }
}

