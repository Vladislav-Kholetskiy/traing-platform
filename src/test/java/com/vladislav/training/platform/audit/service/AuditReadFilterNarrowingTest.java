package com.vladislav.training.platform.audit.service;

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
import com.vladislav.training.platform.audit.domain.AuditEvent;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AuditReadFilterNarrowing}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AuditReadFilterNarrowingTest {

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private AuditQueryService auditQueryService;

    @Test
    void safeFiltersMustOnlyNarrowAlreadyAllowedAuditSlice() {
        AuditAdminReadServiceImpl service = new AuditAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            auditQueryService
        );
        AccessPolicyQueryContext context = listContext();
        AuditEvent eventTypeMatch = auditEvent(
            81L,
            "IMPORT_JOB_LAUNCH",
            "IMPORT_JOB",
            "5201",
            701L,
            Instant.parse("2026-05-09T09:10:00Z")
        );
        AuditEvent eventTypeOther = auditEvent(
            82L,
            "IMPORT_JOB_REVIEW",
            "IMPORT_JOB",
            "5201",
            701L,
            Instant.parse("2026-05-09T09:12:00Z")
        );
        AuditEvent actorAndRangeOther = auditEvent(
            83L,
            "IMPORT_JOB_LAUNCH",
            "IMPORT_JOB",
            "5201",
            702L,
            Instant.parse("2026-05-09T10:30:00Z")
        );

        when(queryContextResolver.resolveAuditEventAdministrationContext(101L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(auditQueryService.findAuditEventsByEntity("IMPORT_JOB", "5201"))
            .thenReturn(List.of(eventTypeMatch, eventTypeOther, actorAndRangeOther));

        List<AuditAdminReadService.AuditEventReadModel> filtered = service.listAdminAuditEvents(
            101L,
            new AuditAdminReadService.AuditEventReadFilter(
                "IMPORT_JOB_LAUNCH",
                "IMPORT_JOB",
                "5201",
                701L,
                Instant.parse("2026-05-09T09:00:00Z"),
                Instant.parse("2026-05-09T10:00:00Z")
            )
        );

        assertThat(filtered)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.id()).isEqualTo(81L);
                assertThat(event.eventType()).isEqualTo("IMPORT_JOB_LAUNCH");
                assertThat(event.entityType()).isEqualTo("IMPORT_JOB");
                assertThat(event.entityId()).isEqualTo("5201");
                assertThat(event.actorUserId()).isEqualTo(701L);
                assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-05-09T09:10:00Z"));
            });

        verify(auditQueryService).findAuditEventsByEntity("IMPORT_JOB", "5201");
        verify(auditQueryService, never()).findAllAuditEvents();
    }

    @Test
    void actorUserIdFilterMustNarrowAuditRowActorWithoutChangingAuthenticatedActor() {
        AuditAdminReadServiceImpl service = new AuditAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            auditQueryService
        );
        AccessPolicyQueryContext context = listContext();
        AuditEvent actor701 = auditEvent(
            91L,
            "NOTIFICATION_SENT",
            "NOTIFICATION",
            "9001",
            701L,
            Instant.parse("2026-05-09T08:00:00Z")
        );
        AuditEvent actor702 = auditEvent(
            92L,
            "NOTIFICATION_SENT",
            "NOTIFICATION",
            "9002",
            702L,
            Instant.parse("2026-05-09T08:05:00Z")
        );

        when(queryContextResolver.resolveAuditEventAdministrationContext(101L)).thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(auditQueryService.findAuditEventsByActorUserId(701L)).thenReturn(List.of(actor701));

        List<AuditAdminReadService.AuditEventReadModel> filtered = service.listAdminAuditEvents(
            101L,
            new AuditAdminReadService.AuditEventReadFilter(
                null,
                null,
                null,
                701L,
                null,
                null
            )
        );

        assertThat(filtered).extracting(AuditAdminReadService.AuditEventReadModel::actorUserId).containsExactly(701L);
        verify(queryContextResolver).resolveAuditEventAdministrationContext(101L);
        verify(auditQueryService).findAuditEventsByActorUserId(701L);
        verify(auditQueryService, never()).findAllAuditEvents();
        verify(auditQueryService, never()).findAuditEventsByEntity("NOTIFICATION", "9002");
    }

    private AccessPolicyQueryContext listContext() {
        return new AccessPolicyQueryContext(
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
    }

    private AuditEvent auditEvent(
        Long id,
        String eventType,
        String entityType,
        String entityId,
        Long actorUserId,
        Instant occurredAt
    ) {
        return new AuditEvent(
            id,
            new AuditEventType(eventType),
            entityType,
            entityId,
            actorUserId,
            occurredAt,
            null,
            null,
            null,
            "corr-" + id,
            "req-" + id,
            occurredAt.plusSeconds(5)
        );
    }
}
