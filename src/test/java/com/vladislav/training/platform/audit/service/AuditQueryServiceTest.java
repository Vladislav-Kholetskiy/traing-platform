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
import com.vladislav.training.platform.audit.domain.AuditPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code AuditQuery}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    private static final Path QUERY_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditQueryServiceImpl.java"
    );
    private static final Path READ_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private AuditQueryService auditQueryService;

    @Test
    void auditReadFlowMustUseOnlyAuditFactsWithoutImportNotificationOrOwnerJoins() throws Exception {
        String querySource = Files.readString(QUERY_IMPL);
        String readSource = Files.readString(READ_SERVICE_IMPL);

        assertThat(querySource)
            .contains("class AuditQueryServiceImpl implements AuditQueryService")
            .doesNotContain("Import")
            .doesNotContain("Notification")
            .doesNotContain("Assignment")
            .doesNotContain("Testing")
            .doesNotContain("Result")
            .doesNotContain("Content")
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository");

        assertThat(readSource)
            .contains("AuditQueryService")
            .doesNotContain("Import")
            .doesNotContain("Notification")
            .doesNotContain("Assignment")
            .doesNotContain("Testing")
            .doesNotContain("Result")
            .doesNotContain("Content")
            .doesNotContain("UserCommandService")
            .doesNotContain("AppUserRepository");
    }

    @Test
    void listAndDetailReturnReadModelsAndFiltersOnlyNarrowVisibleAuditSlice() {
        AuditAdminReadServiceImpl service = new AuditAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            auditQueryService
        );
        AccessPolicyQueryContext listContext = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.AUDIT_EVENT_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-09T09:10:00Z"),
            null,
            null,
            "audit_event",
            null,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        AccessPolicyQueryContext detailContext = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.AUDIT_EVENT_ADMINISTRATION,
            AccessReadType.DETAIL,
            Instant.parse("2026-05-09T09:15:00Z"),
            null,
            null,
            "audit_event",
            81L,
            null,
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.UNSPECIFIED
        );
        AuditEvent matching = auditEvent(81L, "IMPORT_JOB", "5201", 701L, "IMPORT_JOB_LAUNCH");
        AuditEvent nonMatching = auditEvent(82L, "IMPORT_JOB", "5202", 702L, "IMPORT_JOB_LAUNCH");

        when(queryContextResolver.resolveAuditEventAdministrationContext(101L)).thenReturn(listContext);
        when(queryContextResolver.resolveAuditEventAdministrationDetailContext(101L, 81L)).thenReturn(detailContext);
        when(accessSpecificationPolicy.canRead(listContext)).thenReturn(true);
        when(accessSpecificationPolicy.canRead(detailContext)).thenReturn(true);
        when(auditQueryService.findAuditEventsByEntity("IMPORT_JOB", "5201"))
            .thenReturn(List.of(matching));
        when(auditQueryService.findAuditEventById(81L)).thenReturn(matching);

        List<AuditAdminReadService.AuditEventReadModel> events = service.listAdminAuditEvents(
            101L,
            new AuditAdminReadService.AuditEventReadFilter("IMPORT_JOB_LAUNCH", "IMPORT_JOB", "5201")
        );
        AuditAdminReadService.AuditEventReadModel detail = service.findAdminAuditEventById(101L, 81L);

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.id()).isEqualTo(81L);
            assertThat(event.actorUserId()).isEqualTo(701L);
            assertThat(event.eventType()).isEqualTo("IMPORT_JOB_LAUNCH");
            assertThat(event.entityType()).isEqualTo("IMPORT_JOB");
            assertThat(event.entityId()).isEqualTo("5201");
        });
        assertThat(detail.id()).isEqualTo(81L);
        assertThat(detail.eventType()).isEqualTo("IMPORT_JOB_LAUNCH");

        verify(auditQueryService, never()).findAllAuditEvents();
        verify(auditQueryService, never()).findAuditEventsByActorUserId(701L);
        verify(auditQueryService, never()).findAuditEventsByEventType(new AuditEventType("IMPORT_JOB_LAUNCH"));
    }

    private AuditEvent auditEvent(
        Long id,
        String entityType,
        String entityId,
        Long actorUserId,
        String eventType
    ) {
        Instant now = Instant.parse("2026-05-09T09:20:00Z");
        return new AuditEvent(
            id,
            new AuditEventType(eventType),
            entityType,
            entityId,
            actorUserId,
            now,
            new AuditPayload("{\"before\":\"hidden\"}"),
            new AuditPayload("{\"after\":\"hidden\"}"),
            null,
            "corr-" + id,
            "req-" + id,
            now
        );
    }
}
