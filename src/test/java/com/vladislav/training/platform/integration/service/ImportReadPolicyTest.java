package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ImportReadPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ImportReadPolicyTest {

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private ImportQueryService importQueryService;

    @Test
    void denyHappensBeforeMaterializationForImportJobList() {
        ImportAdminReadServiceImpl service = new ImportAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            importQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-09T10:00:00Z"),
            null,
            null,
            "import_job",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        when(queryContextResolver.resolveImportJobAdministrationContext(101L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.listImportJobs(101L, null))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("forbidden");

        verify(queryContextResolver).resolveImportJobAdministrationContext(101L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(importQueryService, never()).findImportJobsByStatus(org.mockito.ArgumentMatchers.any());
        verify(importQueryService, never()).findImportJobsBySourceType(org.mockito.ArgumentMatchers.any());
        verify(importQueryService, never()).findImportJobById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dedicatedImportContourIsUsedForItemDetailAndAnalyticsContoursStayUnused() {
        ImportAdminReadServiceImpl service = new ImportAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            importQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.IMPORT_JOB_ADMINISTRATION,
            AccessReadType.DETAIL,
            Instant.parse("2026-05-09T10:05:00Z"),
            null,
            null,
            "import_job_item",
            91L,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        when(queryContextResolver.resolveImportJobItemAdministrationDetailContext(101L, 91L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(importQueryService.findImportJobItemById(91L)).thenReturn(ImportQueryFixtures.importJobItem(91L, 77L, 0));

        ImportAdminReadService.ImportJobItemReadModel item = service.findImportJobItemById(101L, 91L);

        assertThat(item.id()).isEqualTo(91L);
        assertThat(item.importJobId()).isEqualTo(77L);
        verify(queryContextResolver).resolveImportJobItemAdministrationDetailContext(101L, 91L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(importQueryService).findImportJobItemById(91L);
    }
}
