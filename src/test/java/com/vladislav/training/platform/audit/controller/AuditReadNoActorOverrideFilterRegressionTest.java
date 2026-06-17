package com.vladislav.training.platform.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AuditReadNoActorOverrideFilter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AuditReadNoActorOverrideFilterRegressionTest {

    private static final Path CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );
    private static final Path READ_SERVICE_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );

    @Test
    void actorUserIdQueryParamMustRemainAuditRowFilterAndNeverReplaceAuthenticatedActor() {
        String controllerSource = read(CONTROLLER_PATH);
        String serviceSource = read(READ_SERVICE_PATH);

        assertThat(controllerSource)
            .contains("InteractiveActorResolver")
            .contains("resolveActorUserId()")
            .doesNotContain("@RequestBody")
            .doesNotContain("@PathVariable(\"actorUserId\")");

        assertThat(serviceSource)
            .contains("resolveAuditEventAdministrationContext(actorUserId)")
            .doesNotContain("resolveAuditEventAdministrationContext(filter.actorUserId())")
            .doesNotContain("resolveAuditEventAdministrationContext(effectiveFilter.actorUserId())");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
