package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.integration.personnel.controller.PersonnelExcelImportController;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyRowResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение {@code PersonnelExcelImportControllerAdmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelExcelImportControllerAdmissionTest {

    @Mock
    private PersonnelImportAdmissionService personnelImportAdmissionService;
    @Mock
    private PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade;
    @Mock
    private PersonnelApplyService personnelApplyService;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private CapabilityAdmissionRequest capabilityAdmissionRequest;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(Instant.parse("2026-05-11T21:15:00Z"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PersonnelExcelImportController(
                    personnelImportAdmissionService,
                    personnelWorkbookDryRunFacade,
                    personnelApplyService,
                    capabilityAdmissionRequestFactory
                )
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void dryRunAndApplyEndpointsRemainSeparate() throws Exception {
        when(capabilityAdmissionRequestFactory.createPersonnelExcelDryRun()).thenReturn(capabilityAdmissionRequest);
        when(personnelWorkbookDryRunFacade.dryRun(any())).thenReturn(List.of(
            new PersonnelPlan(2, "1001", PersonnelPlanOutcomeCode.NO_CHANGE, "ACTIVE", List.of(), List.of())
        ));
        when(personnelApplyService.apply(any())).thenReturn(new PersonnelApplyResult(List.of(
            new PersonnelApplyRowResult(2, "1001", "NO_CHANGE", "ACTIVE", List.of(), List.of())
        )));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/dry-run")
                .file(new MockMultipartFile("file", "dry.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "dry".getBytes())))
            .andExpect(status().isOk());

        verify(personnelWorkbookDryRunFacade).dryRun(any());
        verify(personnelApplyService, never()).apply(any());

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/apply")
                .file(new MockMultipartFile("file", "apply.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "apply".getBytes())))
            .andExpect(status().isOk());

        verify(personnelApplyService).apply(any());
    }

    @Test
    void controllerSourceDoesNotUseImportJobLaunchVocabulary() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .contains("/dry-run")
            .contains("/apply")
            .doesNotContain("IMPORT_JOB_LAUNCH");
    }
}
