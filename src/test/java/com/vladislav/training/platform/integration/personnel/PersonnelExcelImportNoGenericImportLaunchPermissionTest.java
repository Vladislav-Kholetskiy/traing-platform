package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.application.policy.DefaultCapabilityAdmissionPolicy;
import com.vladislav.training.platform.audit.service.SystemActorResolver;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code PersonnelExcelImportNoGenericImportLaunchPermission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelExcelImportNoGenericImportLaunchPermissionTest {

    private static final Long SYSTEM_ACTOR_USER_ID = 9900L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-11T11:00:00Z");

    private final InteractiveActorResolver interactiveActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private SystemActorResolver systemActorResolver;

    private DefaultCapabilityAdmissionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DefaultCapabilityAdmissionPolicy(
            userOrgFoundationStateReadService,
            accessFoundationStateReadService,
            interactiveActorResolver
        );
        policy.setSystemActorResolver(systemActorResolver);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void technicalSystemShortcutRemainsStrictlyScopedToImportJobLaunch() {
        when(systemActorResolver.resolveSystemActorUserId()).thenReturn(SYSTEM_ACTOR_USER_ID);
        when(userOrgFoundationStateReadService.findActorCommandFoundationState(SYSTEM_ACTOR_USER_ID, FIXED_INSTANT)).thenReturn(
            new UserOrgFoundationStateReadService.ActorCommandFoundationState(SYSTEM_ACTOR_USER_ID, true, Set.of())
        );

        CapabilityAdmissionRequest importLaunchRequest = new CapabilityAdmissionRequest(
            SYSTEM_ACTOR_USER_ID,
            CapabilityOperationCodes.IMPORT_JOB_LAUNCH,
            CapabilityTargetEntityType.IMPORT_JOB,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatCode(() -> policy.check(importLaunchRequest)).doesNotThrowAnyException();

        verify(systemActorResolver).resolveSystemActorUserId();
    }

    @Test
    void importJobLaunchShortcutDoesNotAuthorizePersonnelApply() {
        CapabilityAdmissionRequest personnelApplyRequest = new CapabilityAdmissionRequest(
            SYSTEM_ACTOR_USER_ID,
            CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY,
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );

        assertThatThrownBy(() -> policy.check(personnelApplyRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Authenticated principal is required");

        verify(systemActorResolver, never()).resolveSystemActorUserId();
        verifyNoInteractions(accessFoundationStateReadService);
    }
}

