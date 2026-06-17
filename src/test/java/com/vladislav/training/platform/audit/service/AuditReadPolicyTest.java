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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AuditReadPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AuditReadPolicyTest {

    private static final Path READ_SERVICE_INTERFACE = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadService.java"
    );
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
    void auditAdminReadFlowMustUseDedicatedAdministrativeContourBeforeMaterialization() {
        assertThat(Files.exists(READ_SERVICE_INTERFACE))
            
            .isTrue();
        assertThat(Files.exists(READ_SERVICE_IMPLEMENTATION))
            
            .isTrue();

        String interfaceSource = read(READ_SERVICE_INTERFACE);
        String implementationSource = read(READ_SERVICE_IMPLEMENTATION);

        assertThat(interfaceSource)
            .contains("listAdminAuditEvents")
            .contains("findAdminAuditEventById");

        assertThat(implementationSource)
            .contains("AccessPolicyQueryContextResolver")
            .contains("AccessSpecificationPolicy")
            .contains("AuditQueryService")
            .contains("resolveAuditEventAdministrationContext")
            .contains("resolveAuditEventAdministrationDetailContext")
            .contains("AccessReadArea.AUDIT_EVENT_ADMINISTRATION")
            .doesNotContain("AccessReadArea.SELF_RESULT_HISTORY")
            .doesNotContain("AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION")
            .doesNotContain("AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS")
            .doesNotContain("AccessReadArea.EXPERT_QUESTION_ANALYTICS")
            .doesNotContain("AuditEventRepository");
    }

    @Test
    void denyPathMustHappenBeforeAuditMaterialization() {
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

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
