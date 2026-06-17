package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.application.policy.DefaultCapabilityAdmissionPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
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
 * Проверяет поведение {@code PersonnelExcelImportAdmissionFailClosed}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelExcelImportAdmissionFailClosedTest {

    private static final Long ACTOR_USER_ID = 202L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-11T10:30:00Z");

    private final InteractiveActorResolver policyActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private InteractiveActorResolver requestFactoryActorResolver;

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
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ACTOR_USER_ID, null, "ROLE_USER"));
        lenient().when(requestFactoryActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
        lenient().when(userOrgFoundationStateReadService.findActorCommandFoundationState(ACTOR_USER_ID, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(ACTOR_USER_ID, true, Set.of("MANAGER"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonAdmittedActorIsDeniedForPersonnelDryRunAndApply() {
        CapabilityAdmissionRequest dryRunRequest = requestFactory.createPersonnelExcelDryRun();
        CapabilityAdmissionRequest applyRequest = requestFactory.createPersonnelExcelApply();

        assertThatThrownBy(() -> policy.check(dryRunRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized");

        assertThatThrownBy(() -> policy.check(applyRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized");
    }

    @Test
    void managerialVisibilityDoesNotBecomePersonnelApplyPermissionAndDeniedPathStaysSideEffectFree() {
        CapabilityAdmissionRequest request = requestFactory.createPersonnelExcelApply();

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized");

        verify(userOrgFoundationStateReadService).findActorCommandFoundationState(ACTOR_USER_ID, FIXED_INSTANT);
        verifyNoMoreInteractions(userOrgFoundationStateReadService);
        verify(accessFoundationStateReadService).findActiveTemporaryRoleIds(ACTOR_USER_ID, FIXED_INSTANT);
        verifyNoMoreInteractions(accessFoundationStateReadService);
    }

    @Test
    void personnelApplyDoesNotReuseImportJobVocabulary() {
        CapabilityAdmissionRequest request = requestFactory.createPersonnelExcelApply();

        assertThatThrownBy(() -> policy.check(request))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized");

        verify(userOrgFoundationStateReadService).findActorCommandFoundationState(ACTOR_USER_ID, FIXED_INSTANT);
        verify(accessFoundationStateReadService).findActiveTemporaryRoleIds(ACTOR_USER_ID, FIXED_INSTANT);
        verifyNoMoreInteractions(accessFoundationStateReadService);
        org.assertj.core.api.Assertions.assertThat(request.operationCode()).isNotEqualTo(CapabilityOperationCodes.IMPORT_JOB_LAUNCH);
        org.assertj.core.api.Assertions.assertThat(request.targetEntityType()).isEqualTo(CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT);
    }

    @Test
    void adminActorIsAdmittedForPersonnelDryRunAndApply() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(ACTOR_USER_ID, null, "ROLE_ADMIN"));
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(ACTOR_USER_ID, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(ACTOR_USER_ID, true, Set.of("ADMIN"))
        );

        CapabilityAdmissionRequest dryRunRequest = requestFactory.createPersonnelExcelDryRun();
        CapabilityAdmissionRequest applyRequest = requestFactory.createPersonnelExcelApply();

        org.assertj.core.api.Assertions.assertThatCode(() -> policy.check(dryRunRequest)).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() -> policy.check(applyRequest)).doesNotThrowAnyException();
    }

    private UtcClock fixedClock() {
        return () -> FIXED_INSTANT;
    }
}

