package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.integration.domain.ImportItemStatus;
import com.vladislav.training.platform.integration.domain.ImportJobStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code ImportQuery}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class ImportQueryServiceTest {

    private static final Path QUERY_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportQueryServiceImpl.java"
    );
    private static final Path READ_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportAdminReadServiceImpl.java"
    );

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private ImportQueryService importQueryService;

    @Test
    void importReadFlowUsesQueryServiceProjectionWithoutProcessingCommandOrOwnerServices() throws Exception {
        String querySource = Files.readString(QUERY_IMPL);
        String readSource = Files.readString(READ_SERVICE_IMPL);

        assertThat(querySource)
            .contains("class ImportQueryServiceImpl implements ImportQueryService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportCommandService")
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository");

        assertThat(readSource)
            .contains("ImportQueryService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportCommandService")
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository");
    }

    @Test
    void listJobsAndItemsReturnReadModelsAndFiltersOnlyNarrowVisibleSlice() {
        ImportAdminReadServiceImpl service = new ImportAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            importQueryService
        );
        AccessPolicyQueryContext jobsContext = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-09T10:10:00Z"),
            null,
            null,
            "import_job",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        AccessPolicyQueryContext itemsContext = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-09T10:15:00Z"),
            null,
            null,
            "import_job_item",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        when(queryContextResolver.resolveImportJobAdministrationContext(101L)).thenReturn(jobsContext);
        when(queryContextResolver.resolveImportJobItemAdministrationContext(101L)).thenReturn(itemsContext);
        when(accessSpecificationPolicy.canRead(jobsContext)).thenReturn(true);
        when(accessSpecificationPolicy.canRead(itemsContext)).thenReturn(true);
        when(importQueryService.findImportJobsBySourceType("HR_CSV")).thenReturn(List.of(
            ImportQueryFixtures.importJob(1L, "HR_CSV", ImportJobStatus.PENDING, 2),
            ImportQueryFixtures.importJob(2L, "LDAP", ImportJobStatus.PENDING, 1)
        ));
        when(importQueryService.findImportJobItemsByJobId(77L)).thenReturn(List.of(
            ImportQueryFixtures.importJobItem(91L, 77L, 0),
            new com.vladislav.training.platform.integration.domain.ImportJobItem(
                92L,
                77L,
                1,
                "APP_USER",
                "EXT-1",
                "EMP-1",
                ImportItemStatus.FAILED,
                null,
                "{\"employeeNumber\":\"EMP-1\"}",
                "INVALID_PAYLOAD",
                "bad payload",
                Instant.parse("2026-05-09T09:05:00Z"),
                Instant.parse("2026-05-09T09:00:00Z"),
                Instant.parse("2026-05-09T09:05:00Z")
            )
        ));

        List<ImportAdminReadService.ImportJobReadModel> jobs = service.listImportJobs(
            101L,
            new ImportAdminReadService.ImportJobReadFilter(ImportJobStatus.PENDING, "HR_CSV")
        );
        List<ImportAdminReadService.ImportJobItemReadModel> items = service.listImportJobItems(
            101L,
            77L,
            new ImportAdminReadService.ImportJobItemReadFilter(ImportItemStatus.FAILED)
        );

        assertThat(jobs).singleElement().satisfies(job -> {
            assertThat(job.id()).isEqualTo(1L);
            assertThat(job.sourceType()).isEqualTo("HR_CSV");
        });
        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(92L);
            assertThat(item.status()).isEqualTo(ImportItemStatus.FAILED);
            assertThat(item.matchedEntityId()).isNull();
        });

        verify(importQueryService, never()).findImportJobById(org.mockito.ArgumentMatchers.any());
        verify(importQueryService, never()).findImportJobItemById(org.mockito.ArgumentMatchers.any());
    }
}
