package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportAuditSupportBoundary} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportAuditSupportBoundaryRegressionTest {

    @Test
    void importContoursMustNotUseAuditReadSideAsStateProof() throws Exception {
        String commandService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
        );
        String processingService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
        );
        String reviewService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
        );
        String commandController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminCommandController.java"
        );
        String readController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminReadController.java"
        );
        String reviewController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportItemReviewController.java"
        );

        assertNoAuditReadSideDependency(commandService);
        assertNoAuditReadSideDependency(processingService);
        assertNoAuditReadSideDependency(reviewService);
        assertNoAuditReadSideDependency(commandController);
        assertNoAuditReadSideDependency(readController);
        assertNoAuditReadSideDependency(reviewController);
    }

    @Test
    void importContoursMustNotTreatAuditAsJobPersistenceOrReviewAuthority() throws Exception {
        String commandService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
        );
        String processingService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
        );
        String reviewService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
        );

        assertNoAuditPersistenceSemantics(commandService);
        assertNoAuditPersistenceSemantics(processingService);
        assertNoAuditPersistenceSemantics(reviewService);
    }

    @Test
    void importFlowsMustNotSilentlyInventAuditCompanionWithoutEstablishedClassification() throws Exception {
        String commandService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
        );
        String processingService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
        );
        String reviewService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
        );

        assertThat(commandService)
            .doesNotContain("CriticalCommandAuditSupport")
            .doesNotContain("AuditService")
            .doesNotContain("recordAudit(")
            .doesNotContain("recordAuditEvent(")
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload");

        assertThat(processingService)
            .doesNotContain("CriticalCommandAuditSupport")
            .doesNotContain("AuditService")
            .doesNotContain("recordAudit(")
            .doesNotContain("recordAuditEvent(")
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload");

        assertThat(reviewService)
            .doesNotContain("CriticalCommandAuditSupport")
            .doesNotContain("AuditService")
            .doesNotContain("recordAudit(")
            .doesNotContain("recordAuditEvent(")
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload");
    }

    @Test
    void auditReadSideMustRemainConsumerOnlyRelativeToImportContours() throws Exception {
        String auditAdminReadService = read(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
        );
        String auditQueryService = read(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditQueryServiceImpl.java"
        );
        String auditAdminController = read(
            "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
        );

        assertThat(auditAdminReadService)
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("ImportJobRepository")
            .doesNotContain("ImportJobItemRepository")
            .doesNotContain("findImport")
            .doesNotContain("processImport")
            .doesNotContain("reviewImport");

        assertThat(auditQueryService)
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("ImportJobRepository")
            .doesNotContain("ImportJobItemRepository")
            .doesNotContain("findImport")
            .doesNotContain("processImport")
            .doesNotContain("reviewImport");

        assertThat(auditAdminController)
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("ImportJobRepository")
            .doesNotContain("ImportJobItemRepository");
    }

    private void assertNoAuditReadSideDependency(String source) {
        assertThat(source)
            .doesNotContain("AuditAdminReadService")
            .doesNotContain("AuditQueryService")
            .doesNotContain("AuditAdminReadController")
            .doesNotContain("AuditEventReadResponse")
            .doesNotContain("findAudit")
            .doesNotContain("readAudit")
            .doesNotContain("audit read DTO")
            .doesNotContain("audit_event");
    }

    private void assertNoAuditPersistenceSemantics(String source) {
        assertThat(source)
            .doesNotContain("AuditEvent")
            .doesNotContain("auditEvent")
            .doesNotContain("audit_event")
            .doesNotContain("AuditQuery")
            .doesNotContain("AuditAdminRead")
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload")
            .doesNotContain("retry queue")
            .doesNotContain("repair log")
            .doesNotContain("reconcile")
            .doesNotContain("backfill");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath));
    }
}
