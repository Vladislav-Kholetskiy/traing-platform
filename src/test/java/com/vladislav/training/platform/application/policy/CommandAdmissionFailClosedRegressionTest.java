package com.vladislav.training.platform.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
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
 * Проверяет, что {@code CommandAdmissionFailClosed} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
@ExtendWith(MockitoExtension.class)
class CommandAdmissionFailClosedRegressionTest {

    private static final Long UNAUTHORIZED_ACTOR_ID = 202L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-07T20:00:00Z");

    private final InteractiveActorResolver policyActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private com.vladislav.training.platform.application.actor.InteractiveActorResolver requestFactoryActorResolver;

    private DefaultCapabilityAdmissionPolicy policy;
    private CapabilityAdmissionRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        policy = new DefaultCapabilityAdmissionPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            policyActorResolver
        );
        requestFactory = new CapabilityAdmissionRequestFactory(requestFactoryActorResolver, fixedClock());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(UNAUTHORIZED_ACTOR_ID, null, "ROLE_USER"));
        lenient().when(requestFactoryActorResolver.resolveActorUserId()).thenReturn(UNAUTHORIZED_ACTOR_ID);
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
    void unauthorizedActorRemainsDeniedForAdministrativeCommandRequestsEvenAfterNarrowAllowExists() {
        List<CapabilityAdmissionRequest> requests = List.of(
            requestFactory.createNotificationRuleCreate(),
            requestFactory.createNotificationRuleUpdate(701L),
            requestFactory.createNotificationRuleEnable(702L),
            requestFactory.createNotificationRuleDisable(703L),
            requestFactory.createImportJobLaunch()
        );

        assertThat(requests)
            .extracting(CapabilityAdmissionRequest::operationCode)
            .containsExactly(
                CapabilityOperationCodes.NOTIFICATION_RULE_CREATE,
                CapabilityOperationCodes.NOTIFICATION_RULE_UPDATE,
                CapabilityOperationCodes.NOTIFICATION_RULE_ENABLE,
                CapabilityOperationCodes.NOTIFICATION_RULE_DISABLE,
                CapabilityOperationCodes.IMPORT_JOB_LAUNCH
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
                CapabilityTargetEntityType.IMPORT_JOB.name()
            )
            .doesNotContain(
                "GENERIC_ADMIN",
                "OPERATIONAL_TABLE",
                "DATABASE_TABLE",
                "OWNER_TABLE",
                "AUDIT_EVENT"
            );

        for (CapabilityAdmissionRequest request : requests) {
            assertThatThrownBy(() -> policy.check(request))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("not authorized");
        }
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

    private UtcClock fixedClock() {
        return () -> FIXED_INSTANT;
    }
}

