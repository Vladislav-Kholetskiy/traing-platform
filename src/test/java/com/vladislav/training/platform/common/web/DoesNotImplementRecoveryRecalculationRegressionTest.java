package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code DoesNotImplementRecoveryRecalculation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class DoesNotImplementRecoveryRecalculationRegressionTest {

    private static final Path MAIN_SRC = Path.of("src/main/java/com/vladislav/training/platform");
    private static final List<String> WAVE7_RUNTIME_MARKERS = List.of(
        "Recovery",
        "Reconciliation",
        "Reconcile",
        "Repair",
        "Rebuild",
        "Backfill",
        "Replay"
    );

    @Test
    void administrativeNotificationIntegrationAuditPackagesMustNotImplementMaintenanceRuntimeClasses() throws Exception {
        assertNoMaintenanceClassNames(Path.of("src/main/java/com/vladislav/training/platform/notification"));
        assertNoMaintenanceClassNames(Path.of("src/main/java/com/vladislav/training/platform/integration"));
        assertNoMaintenanceClassNames(Path.of("src/main/java/com/vladislav/training/platform/audit"));
    }

    @Test
    void administrativeControllersMustNotExposeRecoveryOrRebuildRoutes() throws Exception {
        for (Path controller : listJavaFiles(Path.of("src/main/java/com/vladislav/training/platform/notification/controller"))) {
            assertNoRecoveryRoutes(read(controller));
        }
        for (Path controller : listJavaFiles(Path.of("src/main/java/com/vladislav/training/platform/integration/controller"))) {
            assertNoRecoveryRoutes(read(controller));
        }
        for (Path controller : listJavaFiles(Path.of("src/main/java/com/vladislav/training/platform/audit/controller"))) {
            assertNoRecoveryRoutes(read(controller));
        }
    }

    @Test
    void notificationAndImportRuntimeMustStayCurrentOperationalOnly() throws Exception {
        String notificationRuleService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));
        String notificationDispatchService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationDispatchServiceImpl.java"
        ));
        String importProcessingService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
        ));
        String importReviewService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
        ));

        assertThat(notificationRuleService)
            .doesNotContain("backfill")
            .doesNotContain("rebuild")
            .doesNotContain("repair")
            .doesNotContain("reconcile")
            .doesNotContain("recover")
            .doesNotContain("missed-window")
            .doesNotContain("historical notification")
            .doesNotContain("findNotificationsScheduledAtOrBefore(");

        assertThat(notificationDispatchService)
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AssignmentStatusRecalculationScheduler")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("@Scheduled")
            .doesNotContain("cron")
            .doesNotContain("fixedDelay");

        assertThat(importProcessingService)
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AssignmentStatusRecalculationScheduler")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("ResultRecordingIdempotentReplayValidator")
            .doesNotContain("AuditQueryService")
            .doesNotContain("audit read")
            .doesNotContain("reconcile")
            .doesNotContain("repair")
            .doesNotContain("backfill")
            .doesNotContain("@Scheduled");

        assertThat(importReviewService)
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AssignmentStatusRecalculationScheduler")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AuditQueryService")
            .doesNotContain("reconcile")
            .doesNotContain("repair")
            .doesNotContain("backfill")
            .doesNotContain("@Scheduled");
    }

    @Test
    void auditReadRuntimeMustRemainConsumerOnlyAndMustNotRepairFactsOrDriveAdministrativeMutations() throws Exception {
        String auditAdminReadService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
        ));
        String auditQueryService = read(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditQueryServiceImpl.java"
        ));
        String auditAdminController = read(Path.of(
            "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
        ));

        assertThat(auditAdminReadService)
            .doesNotContain("recordAuditEvent(")
            .doesNotContain("saveAuditEvent(")
            .doesNotContain("delete")
            .doesNotContain("update")
            .doesNotContain("replay")
            .doesNotContain("backfill")
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("NotificationRuleService")
            .doesNotContain("CapabilityAdmissionPolicy");

        assertThat(auditQueryService)
            .doesNotContain("recordAuditEvent(")
            .doesNotContain("saveAuditEvent(")
            .doesNotContain("delete")
            .doesNotContain("update")
            .doesNotContain("replay")
            .doesNotContain("backfill")
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("NotificationRuleService");

        assertThat(auditAdminController)
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("NotificationRuleService")
            .doesNotContain("/rebuild")
            .doesNotContain("/recovery")
            .doesNotContain("/repair")
            .doesNotContain("/backfill")
            .doesNotContain("/replay");
    }

    private void assertNoMaintenanceClassNames(Path root) throws IOException {
        for (Path file : listJavaFiles(root)) {
            if (file.getFileName().toString().equals("package-info.java")) {
                continue;
            }
            String fileName = file.getFileName().toString();
            for (String marker : WAVE7_RUNTIME_MARKERS) {
                assertThat(fileName).doesNotContain(marker);
            }
        }
    }

    private void assertNoRecoveryRoutes(String source) {
        assertThat(source)
            .doesNotContain("/rebuild")
            .doesNotContain("/recovery")
            .doesNotContain("/recover")
            .doesNotContain("/reconcile")
            .doesNotContain("/repair")
            .doesNotContain("/backfill")
            .doesNotContain("/replay");
    }

    private List<Path> listJavaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}

