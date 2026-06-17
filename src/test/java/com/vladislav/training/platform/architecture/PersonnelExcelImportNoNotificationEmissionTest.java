package com.vladislav.training.platform.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code PersonnelExcelImportNoNotificationEmission}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class PersonnelExcelImportNoNotificationEmissionTest {

    @Test
    void personnelContourHasNoNotificationOrAuditReadAuthorizationDependency() throws Exception {
        String source = readSources(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel"
        ));

        assertThat(source)
            .doesNotContain("NotificationCommandService")
            .doesNotContain("NotificationDispatchService")
            .doesNotContain("NotificationRepository")
            .doesNotContain("Notification")
            .doesNotContain("AuditQueryService")
            .doesNotContain("AuditRead")
            .doesNotContain("audit replay")
            .doesNotContain("auditReplay")
            .doesNotContain("audit authorization");
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
