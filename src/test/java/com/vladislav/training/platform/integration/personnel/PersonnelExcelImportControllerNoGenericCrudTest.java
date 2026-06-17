package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelExcelImportControllerNoGenericCrud}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelExcelImportControllerNoGenericCrudTest {

    @Test
    void controllerHasNoGenericTargetRouteAndNoImportJobBridge() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .contains("/api/v1/admin/import/personnel-excel")
            .contains("/dry-run")
            .contains("/apply")
            .doesNotContain("/api/v1/admin/import/{target}")
            .doesNotContain("ImportCommandService")
            .doesNotContain("import_job")
            .doesNotContain("import_job_item");
    }

    @Test
    void controllerDoesNotReferenceOwnerServicesRepositoriesAuditNotificationOrMaintenance() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .doesNotContain("UserAdministrationCommandService")
            .doesNotContain("AccessAdministrationCommandService")
            .doesNotContain("Repository")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(")
            .doesNotContain("Audit")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance");
    }

    @Test
    void integrationPersonnelLayerHasNoOwnerPersistenceImportsOrDirectRepositoryMutation() throws Exception {
        String source = readSources(Path.of("src/main/java/com/vladislav/training/platform/integration/personnel"));

        assertThat(source)
            .doesNotContain("userorg.infrastructure.persistence")
            .doesNotContain("access.infrastructure.persistence")
            .doesNotContain("SpringData")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
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
