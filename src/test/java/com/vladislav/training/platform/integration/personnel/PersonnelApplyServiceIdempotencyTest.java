package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPositionMapping;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelChangePlanner;
import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelOwnerMutationExecutor;
import com.vladislav.training.platform.integration.personnel.service.PersonnelRowInterpreter;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookParser;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
/**
 * Проверяет поведение {@code PersonnelApplyServiceIdempotency}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelApplyServiceIdempotencyTest {

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
    private com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private PersonnelCurrentStateReader personnelCurrentStateReader;

    @Test
    void unchangedRowReturnsNoChangeAndDoesNotCallOwnerMutationMethods() {
        PersonnelApplyService personnelApplyService = new PersonnelApplyService(
            currentStateReaderProvider,
            personnelWorkbookParser,
            personnelRowInterpreter,
            personnelChangePlanner,
            personnelOwnerMutationExecutor,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory
        );
        PersonnelRow row = row();
        PersonnelBusinessIntent intent = intent();
        PersonnelPlan plan = new PersonnelPlan(
            2,
            "1001",
            PersonnelPlanOutcomeCode.NO_CHANGE,
            "ACTIVE",
            List.of(),
            List.of()
        );
        when(capabilityAdmissionRequestFactory.createPersonnelExcelApply()).thenReturn(org.mockito.Mockito.mock(CapabilityAdmissionRequest.class));
        when(personnelWorkbookParser.parse(any())).thenReturn(
            new PersonnelWorkbookParseResult(List.of(row), List.of())
        );
        when(currentStateReaderProvider.getIfAvailable()).thenReturn(personnelCurrentStateReader);
        when(personnelRowInterpreter.interpret(row)).thenReturn(intent);
        when(personnelChangePlanner.plan(eq(intent), any())).thenReturn(plan);

        PersonnelApplyResult result = personnelApplyService.apply(new byte[] {1});

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().outcomeCode()).isEqualTo("NO_CHANGE");
        verify(capabilityAdmissionRequestFactory).createPersonnelExcelApply();
        verify(personnelImportAdmissionService).checkApplyAdmission(any(CapabilityAdmissionRequest.class));
        verifyNoInteractions(personnelOwnerMutationExecutor);
    }

    private PersonnelRow row() {
        return new PersonnelRow(
            2,
            "1001",
            "Ivanov",
            "Ivan",
            "Ivanovich",
            "ACTIVE",
            "HQ",
            "DEV",
            null,
            null,
            null,
            null,
            null
        );
    }

    private PersonnelBusinessIntent intent() {
        return new PersonnelBusinessIntent(
            2,
            "1001",
            null,
            true,
            com.vladislav.training.platform.integration.personnel.model.PersonnelEmploymentAction.ENSURE_ACTIVE,
            "ACTIVE",
            "HQ",
            new PersonnelPositionMapping("DEV", Set.of("ROLE_USER"), false, "SELF", false, true),
            null
        );
    }
}
