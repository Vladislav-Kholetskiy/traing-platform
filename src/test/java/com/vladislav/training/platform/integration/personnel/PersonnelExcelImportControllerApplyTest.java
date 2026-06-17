package com.vladislav.training.platform.integration.personnel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.integration.personnel.controller.PersonnelExcelImportController;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyRowResult;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
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
 * Проверяет поведение {@code PersonnelExcelImportControllerApply}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelExcelImportControllerApplyTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-11T21:00:00Z");

    @Mock
    private PersonnelImportAdmissionService personnelImportAdmissionService;
    @Mock
    private PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade;
    @Mock
    private PersonnelApplyService personnelApplyService;
    @Mock
    private CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
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
    void applyEndpointExistsAndValidMultipartRequestDelegatesToApplyService() throws Exception {
        when(personnelApplyService.apply(any())).thenReturn(new PersonnelApplyResult(List.of(
            new PersonnelApplyRowResult(
                2,
                "1001",
                "SUCCESS",
                "UPDATE_EXISTING",
                "ACTIVE",
                List.of(),
                List.of("OPEN_PRIMARY_ORG_ASSIGNMENT", "OPEN_ROLE_ASSIGNMENT"),
                null
            )
        )));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/apply")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-apply".getBytes()
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].rowNumber").value(2))
            .andExpect(jsonPath("$.rows[0].employeeNumber").value("1001"))
            .andExpect(jsonPath("$.rows[0].outcomeCode").value("SUCCESS"))
            .andExpect(jsonPath("$.rows[0].decision").value("UPDATE_EXISTING"))
            .andExpect(jsonPath("$.rows[0].targetUserStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.rows[0].appliedMutationTypes[0]").value("OPEN_PRIMARY_ORG_ASSIGNMENT"))
            .andExpect(jsonPath("$.rows[0].appliedMutationTypes[1]").value("OPEN_ROLE_ASSIGNMENT"));

        verify(personnelApplyService).apply(eq("xlsx-apply".getBytes()));
    }

    @Test
    void createUserApplyResponseExposesCreateDecisionAndCreatedUserId() throws Exception {
        when(personnelApplyService.apply(any())).thenReturn(new PersonnelApplyResult(List.of(
            new PersonnelApplyRowResult(
                2,
                "7001",
                "SUCCESS",
                "CREATE_USER",
                "ACTIVE",
                List.of(),
                List.of("CREATE_USER", "OPEN_PRIMARY_ORG_ASSIGNMENT"),
                501L
            )
        )));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/apply")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-apply".getBytes()
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows[0].decision").value("CREATE_USER"))
            .andExpect(jsonPath("$.rows[0].createdUserId").value(501L))
            .andExpect(jsonPath("$.rows[0].appliedMutationTypes[0]").value("CREATE_USER"));
    }

    @Test
    void applyMissingFileReturnsClientError() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/apply"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void applyNonMultipartRequestReturnsClientError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import/personnel-excel/apply")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void applyFailureIsConvertedToSafeApiErrorWithoutStacktraceLeak() throws Exception {
        when(personnelApplyService.apply(any()))
            .thenThrow(new IllegalArgumentException("Unsupported temporary replacement semantics"));

        mockMvc.perform(multipart("/api/v1/admin/import/personnel-excel/apply")
                .file(new MockMultipartFile(
                    "file",
                    "personnel.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx-apply".getBytes()
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unsupported temporary replacement semantics"))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
