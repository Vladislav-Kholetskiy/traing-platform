package com.vladislav.training.platform.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AuditReadFilterPayloadOmission} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AuditReadFilterPayloadOmissionRegressionTest {

    private static final Path RESPONSE_DTO = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/dto/AuditEventReadResponse.java"
    );
    private static final Path CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );

    @Test
    void addingFiltersMustNotExposePayloadFieldsOrRawJson() {
        assertThat(read(RESPONSE_DTO))
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload")
            .doesNotContain("JsonNode")
            .doesNotContain("Map<String, Object>");

        assertThat(read(CONTROLLER))
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
