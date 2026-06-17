package com.vladislav.training.platform.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение контроллера {@code Audit}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
class AuditControllerTest {

    private static final Path CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );

    @Test
    void auditControllerPublishesOnlyDedicatedGetListAndDetailRoutes() {
        assertThat(Files.exists(CONTROLLER_PATH))
            
            .isTrue();

        String source = read(CONTROLLER_PATH);

        assertThat(source)
            .contains("@RestController")
            .contains("@RequestMapping(\"/api/v1/admin/audit-events\")")
            .contains("@GetMapping")
            .contains("@GetMapping(\"/{auditEventId}\")")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("AuditEventRepository")
            .doesNotContain("AuditEventEntity");
    }

    @Test
    void controllerMustResolveActorFromTrustedInteractiveContextAndReturnDtoReadModel() {
        assertThat(Files.exists(CONTROLLER_PATH))
            
            .isTrue();

        String source = read(CONTROLLER_PATH);

        assertThat(source)
            .contains("InteractiveActorResolver")
            .contains("AuditEventReadResponse")
            .doesNotContain("@RequestParam(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"initiatedByUserId\")")
            .doesNotContain("@RequestParam(\"userId\")")
            .doesNotContain("@RequestBody");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
