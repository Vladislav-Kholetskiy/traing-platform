package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportProcessingServiceNoAuditAsOwnerProof} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportProcessingServiceNoAuditAsOwnerProofRegressionTest {

    private static final Path IMPORT_PROCESSING_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
    );

    @Test
    void processingImplDoesNotUseAuditAsSubstituteForOwnerCommandProof() throws Exception {
        String source = Files.readString(IMPORT_PROCESSING_SERVICE_IMPL);

        assertThat(source)
            .contains("UserCommandService")
            .contains("userCommandService.updateUser(")
            .doesNotContain("Audit")
            .doesNotContain("audit")
            .doesNotContain("recordAudit(")
            .doesNotContain("auditRepository")
            .doesNotContain("auditService");
    }
}
