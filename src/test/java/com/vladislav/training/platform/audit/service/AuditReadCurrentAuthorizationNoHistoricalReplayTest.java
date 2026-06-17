package com.vladislav.training.platform.audit.service;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AuditReadCurrentAuthorizationNoHistoricalReplay}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AuditReadCurrentAuthorizationNoHistoricalReplayTest {

    private static final Path READ_SERVICE_IMPLEMENTATION = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );

    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private AuditQueryService auditQueryService;

    @Test
    void auditReadMustUseCurrentAuthorizationOnlyBeforeQueryMaterialization() {
        AuditAdminReadServiceImpl service = new AuditAdminReadServiceImpl(
            accessSpecificationPolicy,
            queryContextResolver,
            auditQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.AUDIT_EVENT_ADMINISTRATION,
            AccessReadType.LIST,
            Instant.parse("2026-05-10T08:00:00Z"),
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

        assertThatThrownBy(() -> service.listAdminAuditEvents(101L, null))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("forbidden");

        verify(queryContextResolver).resolveAuditEventAdministrationContext(101L);
        verify(accessSpecificationPolicy).canRead(context);
        verify(auditQueryService, never()).findAllAuditEvents();
        verify(auditQueryService, never()).findAuditEventById(org.mockito.ArgumentMatchers.any());
        verify(auditQueryService, never()).findAuditEventsByActorUserId(org.mockito.ArgumentMatchers.any());
        verify(auditQueryService, never()).findAuditEventsByEntity(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void sourceMustNotReplayHistoricalAuthorityFromAuditFacts() {
        String source = read(READ_SERVICE_IMPLEMENTATION);

        assertThat(source)
            .contains("AccessSpecificationPolicy")
            .contains("resolveAuditEventAdministrationContext")
            .contains("resolveAuditEventAdministrationDetailContext")
            .contains("findAuditEventsByActorUserId(filter.actorUserId())")
            .doesNotContain("resolveAuditEventAdministrationContext(filter.actorUserId())")
            .doesNotContain("resolveAuditEventAdministrationContext(effectiveFilter.actorUserId())")
            .doesNotContain("if (event.actorUserId()")
            .doesNotContain("historical")
            .doesNotContain("replay");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
