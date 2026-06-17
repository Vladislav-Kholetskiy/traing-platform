package com.vladislav.training.platform.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AuditReadNoActorOverride} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AuditReadNoActorOverrideRegressionTest {

    private static final Path CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );

    @Test
    void controllerMustNotAcceptActorOverrideThatExpandsVisibility() {
        String source = read(CONTROLLER_PATH);

        assertThat(source)
            .contains("InteractiveActorResolver")
            .contains("resolveActorUserId()")
            .doesNotContain("@RequestBody")
            .doesNotContain("@PathVariable Long actorUserId")
            .doesNotContain("@PathVariable(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"initiatedByUserId\")")
            .doesNotContain("@RequestParam(\"userId\")");
    }

    @Test
    void actorUserIdPublicFilterIfPresentMustNotReplaceAuthenticatedActor() {
        String source = read(CONTROLLER_PATH);

        assertThat(source)
            .contains("Long browsingActorUserId = interactiveActorResolver.resolveActorUserId()")
            .contains("new AuditAdminReadService.AuditEventReadFilter(")
            .doesNotContain("@PathVariable Long actorUserId")
            .doesNotContain("@PathVariable(\"actorUserId\")")
            .doesNotContain("initiatedByUserId")
            .doesNotContain("userId");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
