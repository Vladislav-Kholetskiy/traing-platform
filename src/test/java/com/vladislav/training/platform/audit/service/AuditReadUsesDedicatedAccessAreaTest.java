package com.vladislav.training.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AuditReadUsesDedicatedAccessArea}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AuditReadUsesDedicatedAccessAreaTest {

    private static final Path READ_SERVICE_IMPLEMENTATION = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );
    private static final Path QUERY_CONTEXT_RESOLVER = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
    );
    private static final Path READ_CONTOUR = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessReadArea.java"
    );
    private static final Path ACCESS_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
    );

    @Test
    void auditReadMustUseDedicatedAuditEventAdministrationContourOnly() {
        assertThat(read(READ_CONTOUR))
            .contains("AUDIT_EVENT_ADMINISTRATION")
            .doesNotContain("ADMIN_ALL");

        assertThat(read(READ_SERVICE_IMPLEMENTATION))
            .contains("AccessReadArea.AUDIT_EVENT_ADMINISTRATION")
            .doesNotContain("AccessReadArea.SELF_RESULT_HISTORY")
            .doesNotContain("AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION")
            .doesNotContain("AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS")
            .doesNotContain("AccessReadArea.EXPERT_QUESTION_ANALYTICS");
    }

    @Test
    void listAndDetailContextBuildersMustStayDedicatedToAuditAdministration() {
        assertThat(read(QUERY_CONTEXT_RESOLVER))
            .contains("resolveAuditEventAdministrationContext")
            .contains("resolveAuditEventAdministrationDetailContext")
            .contains("AccessReadArea.AUDIT_EVENT_ADMINISTRATION")
            .doesNotContain("resolveAuditEventAdministrationContext(Long actorUserId, Long actorOverrideUserId)");

        assertThat(read(ACCESS_POLICY))
            .contains("AUDIT_EVENT_ADMINISTRATION_TARGET_FAMILIES = Set.of(\"audit_event\")")
            .contains("context.contour() == AccessReadArea.AUDIT_EVENT_ADMINISTRATION")
            .doesNotContain("ADMIN_ALL");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
