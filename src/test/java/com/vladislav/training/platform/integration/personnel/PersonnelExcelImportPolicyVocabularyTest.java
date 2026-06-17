package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityOperationCode;
import com.vladislav.training.platform.application.policy.CapabilityOperationCodes;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.common.time.UtcClock;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code PersonnelExcelImportPolicyVocabulary}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelExcelImportPolicyVocabularyTest {

    private static final Long ACTOR_USER_ID = 101L;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-11T10:00:00Z");
    private static final Path OPERATION_CODES_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityOperationCodes.java"
    );

    @Mock
    private InteractiveActorResolver interactiveActorResolver;

    private CapabilityAdmissionRequestFactory requestFactory;

    @BeforeEach
    void setUp() {
        UtcClock utcClock = () -> FIXED_INSTANT;
        requestFactory = new CapabilityAdmissionRequestFactory(interactiveActorResolver, utcClock);
        lenient().when(interactiveActorResolver.resolveActorUserId()).thenReturn(ACTOR_USER_ID);
    }

    @Test
    void personnelExcelOperationsAndTargetUseDedicatedVocabulary() throws Exception {
        assertThat(CapabilityOperationCode.valueOf("PERSONNEL_EXCEL_DRY_RUN")).isNotNull();
        assertThat(CapabilityOperationCode.valueOf("PERSONNEL_EXCEL_APPLY")).isNotNull();
        assertThat(CapabilityTargetEntityType.valueOf("PERSONNEL_EXCEL_IMPORT")).isNotNull();

        assertThat(CapabilityOperationCodes.PERSONNEL_EXCEL_DRY_RUN)
            .isEqualTo(CapabilityOperationCode.PERSONNEL_EXCEL_DRY_RUN.code())
            .isNotEqualTo(CapabilityOperationCodes.IMPORT_JOB_LAUNCH);
        assertThat(CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY)
            .isEqualTo(CapabilityOperationCode.PERSONNEL_EXCEL_APPLY.code())
            .isNotEqualTo(CapabilityOperationCodes.IMPORT_JOB_LAUNCH);
        assertThat(CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT)
            .isNotEqualTo(CapabilityTargetEntityType.IMPORT_JOB);

        assertThat(Files.readString(OPERATION_CODES_SOURCE))
            .contains("CapabilityOperationCode.PERSONNEL_EXCEL_DRY_RUN.code()")
            .contains("CapabilityOperationCode.PERSONNEL_EXCEL_APPLY.code()");
    }

    @Test
    void requestFactoryExposesDedicatedPersonnelExcelBuilders() {
        assertThat(Arrays.stream(CapabilityAdmissionRequestFactory.class.getDeclaredMethods()).map(Method::getName))
            .contains("createPersonnelExcelDryRun", "createPersonnelExcelApply");

        CapabilityAdmissionRequest dryRunRequest = requestFactory.createPersonnelExcelDryRun();
        CapabilityAdmissionRequest applyRequest = requestFactory.createPersonnelExcelApply();

        assertThat(dryRunRequest.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(dryRunRequest.operationCode()).isEqualTo(CapabilityOperationCodes.PERSONNEL_EXCEL_DRY_RUN);
        assertThat(dryRunRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT);
        assertThat(dryRunRequest.targetEntityId()).isNull();
        assertThat(dryRunRequest.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
        assertThat(dryRunRequest.requestedAt()).isEqualTo(FIXED_INSTANT);

        assertThat(applyRequest.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(applyRequest.operationCode()).isEqualTo(CapabilityOperationCodes.PERSONNEL_EXCEL_APPLY);
        assertThat(applyRequest.targetEntityType()).isEqualTo(CapabilityTargetEntityType.PERSONNEL_EXCEL_IMPORT);
        assertThat(applyRequest.targetEntityId()).isNull();
        assertThat(applyRequest.payloadContext()).isSameAs(CapabilityAdmissionPayload.Empty.INSTANCE);
        assertThat(applyRequest.requestedAt()).isEqualTo(FIXED_INSTANT);

        assertThat(applyRequest.operationCode()).isNotEqualTo(CapabilityOperationCodes.IMPORT_JOB_LAUNCH);
        assertThat(applyRequest.targetEntityType()).isNotEqualTo(CapabilityTargetEntityType.IMPORT_JOB);
    }
}
