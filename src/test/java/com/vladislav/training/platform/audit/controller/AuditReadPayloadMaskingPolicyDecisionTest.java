package com.vladislav.training.platform.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code AuditReadPayloadMaskingPolicyDecision}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class AuditReadPayloadMaskingPolicyDecisionTest {

    private static final Path RESPONSE_DTO = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/dto/AuditEventReadResponse.java"
    );
    private static final Path CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );
    private static final Path READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadService.java"
    );

    @Test
    void payloadMustRemainOmittedUntilExplicitMaskingPolicyExists() {
        assertThat(read(RESPONSE_DTO))
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload")
            .doesNotContain("payload")
            .doesNotContain("JsonNode")
            .doesNotContain("Map<String, Object>");

        assertThat(read(READ_SERVICE))
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload");
    }

    @Test
    void controllerMustNotExposeRawAuditPayloadWithoutMaskingRules() {
        assertThat(read(CONTROLLER))
            .doesNotContain("payloadBefore")
            .doesNotContain("payloadAfter")
            .doesNotContain("contextPayload")
            .doesNotContain("payload");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
