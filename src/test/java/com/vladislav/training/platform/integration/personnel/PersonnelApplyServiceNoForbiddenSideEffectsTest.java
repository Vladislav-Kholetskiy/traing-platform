package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelApplyServiceNoForbiddenSideEffects}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelApplyServiceNoForbiddenSideEffectsTest {

    @Test
    void integrationPersonnelApplyLayerHasNoDirectJpaPatchImportJobNotificationOrMaintenanceUsage() throws Exception {
        String source = readSources(Path.of("src/main/java/com/vladislav/training/platform/integration/personnel"));

        assertThat(source)
            .doesNotContain("userorg.infrastructure.persistence")
            .doesNotContain("access.infrastructure.persistence")
            .doesNotContain("SpringData")
            .doesNotContain("Repository")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item")
            .doesNotContain("Notification")
            .doesNotContain("AuditRead")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(")
            .doesNotContain("user_id_snapshot")
            .doesNotContain("test_id_snapshot")
            .doesNotContain("test_name_snapshot")
            .doesNotContain("answer_option.is_correct");
    }

    @Test
    void applyControllerRouteMayExistButImportJobBridgeMustNotAppear() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));
        String applyService = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelApplyService.java"
        ));

        assertThat(controller)
            .contains("/apply")
            .doesNotContain("/api/v1/admin/import/{target}")
            .doesNotContain("ImportCommandService");
        assertThat(applyService)
            .doesNotContain("@PostMapping")
            .doesNotContain("@RequestMapping")
            .doesNotContain("ImportCommandService");
    }

    private String readSources(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            StringBuilder builder = new StringBuilder();
            for (Path file : files) {
                builder.append(Files.readString(file)).append('\n');
            }
            return builder.toString();
        }
    }
}
