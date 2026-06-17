package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code PersonnelExcelCreateUserAntiDrift} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class PersonnelExcelCreateUserAntiDriftRegressionTest {

    @Test
    void createUserPathStaysDirectMvpAndDoesNotReuseGenericImportRuntime() throws Exception {
        String source = readSources(Path.of("src/main/java/com/vladislav/training/platform/integration/personnel"));
        String controllerSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        ));

        assertThat(source)
            .doesNotContain("ImportCommandService")
            .doesNotContain("ImportProcessingService")
            .doesNotContain("ImportItemReviewService")
            .doesNotContain("ImportTypedOwnerCommandExecutor")
            .doesNotContain("userorg.infrastructure.persistence")
            .doesNotContain("access.infrastructure.persistence")
            .doesNotContain("SpringData")
            .doesNotContain("Repository")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(")
            .doesNotContain("Notification")
            .doesNotContain("Maintenance")
            .doesNotContain("maintenance");
        assertThat(controllerSource)
            .contains("@RequestMapping(\"/api/v1/admin/import/personnel-excel\")")
            .contains("@PostMapping(\"/dry-run\")")
            .contains("@PostMapping(\"/apply\")")
            .doesNotContain("/api/v1/admin/import/{target}")
            .doesNotContain("/repair")
            .doesNotContain("/rebuild")
            .doesNotContain("/reconcile");
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
