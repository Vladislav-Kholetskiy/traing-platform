package com.vladislav.training.platform.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJob;
import com.vladislav.training.platform.integration.domain.ImportJobItem;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import com.vladislav.training.platform.integration.service.ImportAdminReadService;
import com.vladislav.training.platform.integration.service.ImportCommandService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * Проверяет поведение контроллера {@code Import}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-09T08:00:00Z");
    private static final Path READ_CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminReadController.java"
    );

    @Mock
    private ImportCommandService importCommandService;
    @Mock
    private ImportAdminReadService importAdminReadService;
    @Mock
    private InteractiveActorResolver interactiveActorResolver;
    @Mock
    private UtcClock utcClock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
        lenient().when(interactiveActorResolver.resolveActorUserId()).thenReturn(701L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ImportAdminCommandController(importCommandService),
                new ImportAdminReadController(importAdminReadService, interactiveActorResolver)
            )
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @Test
    void postAdminImportJobsDelegatesToTypedLaunchServiceAndReturnsCreatedResponse() throws Exception {
        when(importCommandService.launchImportJob(any(ImportJob.class), any()))
            .thenReturn(new ImportJob(
                6101L,
                "HR_CSV",
                "hr-feed-2026-05.csv",
                701L,
                ImportJobStatus.PENDING,
                "{\"rows\":2}",
                null,
                null,
                2,
                0,
                0,
                0,
                0,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));

        mockMvc.perform(post("/api/v1/admin/import-jobs")
                .contentType("application/json")
                .content(validLaunchRequestBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.importJobId").value(6101))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalItemCount").value(2))
            .andExpect(jsonPath("$.createdAt").value("2026-05-09T08:00:00Z"))
            .andExpect(jsonPath("$.updatedAt").value("2026-05-09T08:00:00Z"))
            .andExpect(jsonPath("$.initiatedByUserId").doesNotExist())
            .andExpect(jsonPath("$.payload").doesNotExist())
            .andExpect(jsonPath("$.items").doesNotExist());

        ArgumentCaptor<ImportJob> jobCaptor = ArgumentCaptor.forClass(ImportJob.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportJobItem>> itemsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(importCommandService).launchImportJob(jobCaptor.capture(), itemsCaptor.capture());

        ImportJob rawJob = jobCaptor.getValue();
        assertThat(rawJob.id()).isNull();
        assertThat(rawJob.sourceType()).isEqualTo("HR_CSV");
        assertThat(rawJob.sourceRef()).isEqualTo("hr-feed-2026-05.csv");
        assertThat(rawJob.initiatedByUserId()).isNull();
        assertThat(rawJob.payload()).isEqualTo("{\"rows\":2}");
        assertThat(itemsCaptor.getValue()).hasSize(2);
        assertThat(itemsCaptor.getValue())
            .extracting(ImportJobItem::targetEntityType)
            .containsExactly("APP_USER", "APP_USER");
    }

    @Test
    void actorOverrideFieldsAreRejectedAndServiceIsNotCalled() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import-jobs")
                .contentType("application/json")
                .content("""
                    {
                      "sourceType": "HR_CSV",
                      "sourceRef": "hr-feed-2026-05.csv",
                      "payload": "{\\\"rows\\\":1}",
                      "actorUserId": 999,
                      "items": [
                        {
                          "targetEntityType": "APP_USER",
                          "externalId": "EXT-1",
                          "employeeNumber": "EMP-1",
                          "payload": "{\\\"employeeNumber\\\":\\\"EMP-1\\\"}"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(importCommandService, never()).launchImportJob(any(ImportJob.class), any());
    }

    @Test
    void invalidRequestFailsFastWithoutCallingLaunchService() throws Exception {
        mockMvc.perform(post("/api/v1/admin/import-jobs")
                .contentType("application/json")
                .content("""
                    {
                      "sourceType": " ",
                      "sourceRef": "hr-feed-2026-05.csv",
                      "items": []
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        verify(importCommandService, never()).launchImportJob(any(ImportJob.class), any());
    }

    @Test
    void getImportReadRoutesExistAndReturnDtoResponsesWithoutOwnerEntityLeak() throws Exception {
        when(importAdminReadService.listImportJobs(701L, new ImportAdminReadService.ImportJobReadFilter(null, null)))
            .thenReturn(List.of(new ImportAdminReadService.ImportJobReadModel(
                7001L,
                "HR_CSV",
                "hr-feed-2026-05.csv",
                ImportJobStatus.PENDING,
                2,
                0,
                0,
                0,
                0,
                null,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
            )));
        when(importAdminReadService.findImportJobById(701L, 7001L))
            .thenReturn(new ImportAdminReadService.ImportJobReadModel(
                7001L,
                "HR_CSV",
                "hr-feed-2026-05.csv",
                ImportJobStatus.PENDING,
                2,
                0,
                0,
                0,
                0,
                null,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));
        when(importAdminReadService.listImportJobItems(701L, 7001L, new ImportAdminReadService.ImportJobItemReadFilter(null)))
            .thenReturn(List.of(new ImportAdminReadService.ImportJobItemReadModel(
                8001L,
                7001L,
                0,
                "APP_USER",
                "EXT-1",
                "EMP-1",
                ImportItemStatus.PENDING,
                null,
                null,
                null,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
            )));
        when(importAdminReadService.findImportJobItemById(701L, 8001L))
            .thenReturn(new ImportAdminReadService.ImportJobItemReadModel(
                8001L,
                7001L,
                0,
                "APP_USER",
                "EXT-1",
                "EMP-1",
                ImportItemStatus.PENDING,
                null,
                null,
                null,
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
            ));

        mockMvc.perform(get("/api/v1/admin/import-jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(7001))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].payload").doesNotExist())
            .andExpect(jsonPath("$[0].initiatedByUserId").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/import-jobs/7001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7001))
            .andExpect(jsonPath("$.payload").doesNotExist())
            .andExpect(jsonPath("$.appUser").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/import-jobs/7001/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(8001))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].payload").doesNotExist())
            .andExpect(jsonPath("$[0].matchedUser").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/import-job-items/8001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(8001))
            .andExpect(jsonPath("$.payload").doesNotExist())
            .andExpect(jsonPath("$.matchedUser").doesNotExist());
    }

    @Test
    void readControllerUsesTrustedInteractiveActorAndDoesNotDeclareActorOverrideParams() throws Exception {
        String source = Files.readString(READ_CONTROLLER_PATH);

        assertThat(source)
            .contains("InteractiveActorResolver")
            .doesNotContain("@RequestParam(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"initiatedByUserId\")")
            .doesNotContain("@RequestParam(\"userId\")");
    }

    private String validLaunchRequestBody() {
        return """
            {
              "sourceType": "HR_CSV",
              "sourceRef": "hr-feed-2026-05.csv",
              "payload": "{\\\"rows\\\":2}",
              "items": [
                {
                  "targetEntityType": "APP_USER",
                  "externalId": "EXT-1",
                  "employeeNumber": "EMP-1",
                  "payload": "{\\\"employeeNumber\\\":\\\"EMP-1\\\",\\\"externalId\\\":\\\"EXT-1\\\",\\\"lastName\\\":\\\"Ivanov\\\",\\\"firstName\\\":\\\"Ivan\\\",\\\"status\\\":\\\"ACTIVE\\\"}"
                },
                {
                  "targetEntityType": "APP_USER",
                  "externalId": "EXT-2",
                  "employeeNumber": "EMP-2",
                  "payload": "{\\\"employeeNumber\\\":\\\"EMP-2\\\",\\\"externalId\\\":\\\"EXT-2\\\",\\\"lastName\\\":\\\"Petrov\\\",\\\"firstName\\\":\\\"Petr\\\",\\\"status\\\":\\\"ACTIVE\\\"}"
                }
              ]
            }
            """;
    }
}
