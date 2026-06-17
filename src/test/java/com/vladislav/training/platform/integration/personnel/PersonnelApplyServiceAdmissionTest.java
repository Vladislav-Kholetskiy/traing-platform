package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessFoundationStateReadService;
import com.vladislav.training.platform.application.actor.AuthenticatedActorAdapter;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.application.policy.DefaultCapabilityAdmissionPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.integration.personnel.service.PersonnelRowInterpreter;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookParser;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
/**
 * Проверяет поведение {@code PersonnelApplyServiceAdmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyServiceAdmissionTest {

    private static final Long ACTOR_USER_ID = 202L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-11T10:30:00Z");

    private final InteractiveActorResolver policyActorResolver =
        new InteractiveActorResolver(new AuthenticatedActorAdapter());

    @Mock
    private ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider;
    @Mock
    private PersonnelWorkbookParser personnelWorkbookParser;
    @Mock
    private PersonnelRowInterpreter personnelRowInterpreter;
    @Mock
    private PersonnelChangePlanner personnelChangePlanner;
    @Mock
    private PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor;
    @Mock
    private PersonnelImportAdmissionService personnelImportAdmissionService;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;
    @Mock
    private AccessFoundationStateReadService accessFoundationStateReadService;
    @Mock
    private InteractiveActorResolver requestFactoryActorResolver;

    private PersonnelApplyService personnelApplyService;
    private DefaultCapabilityAdmissionPolicy policy;
    private CapabilityAdmissionRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        personnelApplyService = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelWorkbookParser,
            personnelRowInterpreter,
            personnelChangePlanner,
            personnelOwnerMutationExecutor,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory
        );
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
    void applyUsesPersonnelExcelApplyAndDeniedAdmissionStopsBeforeBusinessProcessing() {
        CapabilityAdmissionRequest request = new CapabilityAdmissionRequest(
            ACTOR_USER_ID,
            CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY,
            CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT,
            null,
            CapabilityAdmissionPayload.Empty.INSTANCE,
            FIXED_INSTANT
        );
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(request);
        org.mockito.Mockito.doThrow(new PolicyViolationException("PERSONNEL_EXCEL_APPLY_DENIED", "reserved for a future runtime"))
            .when(personnelImportAdmissionService)
            .checkApplyAdmission(request);

        assertThatThrownBy(() -> personnelApplyService.apply(new byte[] {1, 2, 3}))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("future runtime");

        verify(capabilityAdmissionRequestFactory).createPersonnelExcelApply();
        verify(personnelImportAdmissionService).checkApplyAdmission(request);
        verifyNoInteractions(personnelWorkbookParser, personnelRowInterpreter, personnelChangePlanner, personnelOwnerMutationExecutor);
    }

    @Test
    void personnelApplyDoesNotReuseImportJobLaunchVocabulary() {
        CapabilityAdmissionRequest personnelApplyRequest = requestFactory.createPersonnelExcelApply();

        assertThatThrownBy(() -> policy.check(personnelApplyRequest))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized");

        assertThat(personnelApplyRequest.operationCode()).isEqualTo(CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY);
        assertThat(personnelApplyRequest.operationCode()).isNotEqualTo(CapabilityOperationCodes.IMPORT_JOB_LAUNCH);
        assertThat(personnelApplyRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT);
    }

    @Test
    void applyServiceSourceUsesApplyAdmissionBuilderNotDryRunBuilder() throws Exception {
        Path sourcePath = Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelApplyService.java"
        );

        assertThat(sourcePath).exists();
        assertThat(Files.readString(sourcePath))
            .contains("createPersonnelExcelApply")
            .doesNotContain("createPersonnelExcelDryRun");
    }

    private UtcClock fixedClock() {
        return () -> FIXED_INSTANT;
    }
}

