package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code NotificationAuditSeparation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class NotificationAuditSeparationRegressionTest {

    @Test
    void notificationReadSideMustNotAppendAudit() throws Exception {
        String adminReadService = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadServiceImpl.java"
        );
        String selfReadService = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadServiceImpl.java"
        );
        String adminController = read(
            "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
        );
        String selfController = read(
            "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfReadController.java"
        );

        assertNoAuditWriteSideDependency(adminReadService);
        assertNoAuditWriteSideDependency(selfReadService);
        assertNoAuditWriteSideDependency(adminController);
        assertNoAuditWriteSideDependency(selfController);
    }

    @Test
    void auditReadSideMustNotGrantNotificationReadAccess() throws Exception {
        String auditAdminController = read(
            "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
        );
        String auditAdminReadService = read(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
        );
        String auditQueryService = read(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditQueryServiceImpl.java"
        );
        String auditReadResponse = read(
            "src/main/java/com/vladislav/training/platform/audit/controller/dto/AuditEventReadResponse.java"
        );

        assertThat(auditAdminController).doesNotContain("NotificationQueryService");
        assertThat(auditAdminController).doesNotContain("NotificationAdminReadService");
        assertThat(auditAdminController).doesNotContain("NotificationSelfReadService");
        assertThat(auditAdminController).doesNotContain("NotificationRepository");

        assertThat(auditAdminReadService).doesNotContain("NotificationQueryService");
        assertThat(auditAdminReadService).doesNotContain("NotificationAdminReadService");
        assertThat(auditAdminReadService).doesNotContain("NotificationSelfReadService");
        assertThat(auditAdminReadService).doesNotContain("NotificationRepository");
        assertThat(auditAdminReadService).doesNotContain("findNotification");
        assertThat(auditAdminReadService).doesNotContain("resolveNotification");

        assertThat(auditQueryService).doesNotContain("NotificationQueryService");
        assertThat(auditQueryService).doesNotContain("NotificationRepository");
        assertThat(auditQueryService).doesNotContain("NotificationAdminReadService");
        assertThat(auditQueryService).doesNotContain("NotificationSelfReadService");

        assertThat(auditReadResponse).doesNotContain("NotificationSelfReadResponse");
        assertThat(auditReadResponse).doesNotContain("NotificationAdminReadResponse");
        assertThat(auditReadResponse).doesNotContain("notificationAccess");
        assertThat(auditReadResponse).doesNotContain("grantedRead");
    }

    @Test
    void notificationReadModelMustNotExposeAuditProofSemantics() throws Exception {
        String selfReadDto = read(
            "src/main/java/com/vladislav/training/platform/notification/controller/dto/NotificationSelfReadResponse.java"
        );
        String adminReadDto = read(
            "src/main/java/com/vladislav/training/platform/notification/controller/dto/NotificationAdminReadResponse.java"
        );
        String adminReadService = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadServiceImpl.java"
        );
        String selfReadService = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadServiceImpl.java"
        );

        assertNoAuditProofSemantics(selfReadDto);
        assertNoAuditProofSemantics(adminReadDto);
        assertThat(adminReadService).doesNotContain("verifiedByAudit");
        assertThat(adminReadService).doesNotContain("auditProof");
        assertThat(selfReadService).doesNotContain("verifiedByAudit");
        assertThat(selfReadService).doesNotContain("auditProof");
    }

    @Test
    void notificationReadServicesMustNotDependOnAuditWriterTypes() {
        assertThat(NotificationAdminReadServiceImpl.class.getDeclaredConstructors()).allSatisfy(constructor ->
            assertThat(constructor.getParameterTypes())
                .extracting(Class::getSimpleName)
                .doesNotContain("AuditService", "PersistentAuditService", "CriticalCommandAuditSupport")
        );
        assertThat(NotificationSelfReadServiceImpl.class.getDeclaredConstructors()).allSatisfy(constructor ->
            assertThat(constructor.getParameterTypes())
                .extracting(Class::getSimpleName)
                .doesNotContain("AuditService", "PersistentAuditService", "CriticalCommandAuditSupport")
        );
    }

    private void assertNoAuditWriteSideDependency(String source) {
        assertThat(source).doesNotContain("import com.vladislav.training.platform.audit.service.AuditService");
        assertThat(source).doesNotContain("import com.vladislav.training.platform.audit.service.PersistentAuditService");
        assertThat(source).doesNotContain("import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport");
        assertThat(source).doesNotContain(" recordAudit(");
        assertThat(source).doesNotContain(" recordAuditEvent(");
        assertThat(source).doesNotContain(" appendAudit(");
        assertThat(source).doesNotContain("auditService.");
        assertThat(source).doesNotContain("criticalCommandAuditSupport.");
    }

    private void assertNoAuditProofSemantics(String source) {
        assertThat(source).doesNotContain("auditProof");
        assertThat(source).doesNotContain("auditEventId");
        assertThat(source).doesNotContain("verifiedByAudit");
        assertThat(source).doesNotContain("auditVerified");
        assertThat(source).doesNotContain("auditPayload");
        assertThat(source).doesNotContain("auditStatus");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath));
    }
}
