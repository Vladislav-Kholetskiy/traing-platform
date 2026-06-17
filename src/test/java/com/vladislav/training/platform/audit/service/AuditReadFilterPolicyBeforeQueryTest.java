package com.vladislav.training.platform.audit.service;

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
 * Проверяет поведение {@code AuditReadFilterPolicyBeforeQuery}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AuditReadFilterPolicyBeforeQueryTest {

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private AuditQueryService auditQueryService;

    @Test
    void deniedPolicyMustBlockQueryEvenWhenSafeFiltersArePresent() {
        AuditAdminReadServiceImpl service = new AuditAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            auditQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.AUDIT_EVENT_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-09T09:00:00Z"),
            null,
            null,
            "audit_event",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        when(queryContextResolver.resolveAuditEventAdministrationContext(101L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.listAdminAuditEvents(
            101L,
            new AuditAdminReadService.AuditEventReadFilter(
                "IMPORT_JOB_LAUNCH",
                "IMPORT_JOB",
                "5201",
                701L,
                Instant.parse("2026-05-09T09:00:00Z"),
                Instant.parse("2026-05-09T10:00:00Z")
            )
        ))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("forbidden");

        verify(queryContextResolver).resolveAuditEventAdministrationContext(101L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(auditQueryService, never()).findAllAuditEvents();
        verify(auditQueryService, never()).findAuditEventsByActorUserId(701L);
        verify(auditQueryService, never()).findAuditEventsByEntity("IMPORT_JOB", "5201");
    }
}
