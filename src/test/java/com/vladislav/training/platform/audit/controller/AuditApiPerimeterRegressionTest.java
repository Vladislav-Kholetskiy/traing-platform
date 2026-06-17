package com.vladislav.training.platform.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code AuditApiPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class AuditApiPerimeterRegressionTest {

    private static final Path AUDIT_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );

    @Test
    void auditApiSurfacePublishesOnlyDedicatedAdminReadRoutes() throws IOException {
        List<Path> controllerSources = productionAuditControllerSources();

        assertThat(controllerSources)
            
            .contains(AUDIT_CONTROLLER);

        String controllerSource = read(AUDIT_CONTROLLER);
        assertThat(controllerSource)
            .contains("/api/v1/admin/audit-events")
            .doesNotContain("/api/v1/admin/audit-repair/")
            .doesNotContain("/api/v1/admin/audit-rebuild/")
            .doesNotContain("/api/v1/admin/recovery/")
            .doesNotContain("/api/v1/admin/tables/")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");

        for (Path sourcePath : controllerSources) {
            String source = read(sourcePath);
            assertThat(source)
                .doesNotContain("/api/v1/admin/audit-repair/")
                .doesNotContain("/api/v1/admin/audit-rebuild/")
                .doesNotContain("/api/v1/admin/recovery/")
                .doesNotContain("/api/v1/admin/tables/")
                .doesNotContain("GenericAuditController")
                .doesNotContain("AuditCrudController");
        }
    }

    private List<Path> productionAuditControllerSources() throws IOException {
        Path root = Path.of("src/main/java/com/vladislav/training/platform/audit/controller");
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                .filter(path -> path.toString().endsWith("Controller.java"))
                .toList();
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
