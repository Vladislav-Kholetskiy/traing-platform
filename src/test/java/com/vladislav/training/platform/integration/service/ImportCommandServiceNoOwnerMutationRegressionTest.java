package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportCommandServiceNoOwnerMutation} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportCommandServiceNoOwnerMutationRegressionTest {

    private static final Path IMPORT_COMMAND_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
    );

    @Test
    void importCommandServiceImplMustExistAndStayOwnerMutationFree() throws Exception {
        assertThatCode(() -> Files.readString(IMPORT_COMMAND_SERVICE_IMPL)).doesNotThrowAnyException();
        String source = Files.readString(IMPORT_COMMAND_SERVICE_IMPL);

        assertThat(source)
            .doesNotContain("UserRepository")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("ResultRepository")
            .doesNotContain("CourseRepository")
            .doesNotContain("NotificationRepository")
            .doesNotContain("recordResult")
            .doesNotContain("submitAttempt")
            .doesNotContain("closeAssignment")
            .doesNotContain("patchOwner")
            .doesNotContain("databaseTable")
            .doesNotContain("CriticalCommandAuditSupport")
            .doesNotContain("recordAudit(")
            .doesNotContain("@RestController")
            .doesNotContain("@RequestMapping")
            .doesNotContain("NotificationScheduler")
            .doesNotContain("DeadlineReminderScheduler");
    }

    @Test
    void rawMaterializationMustNotSeedTerminalProcessingStates() throws Exception {
        String source = Files.readString(IMPORT_COMMAND_SERVICE_IMPL);

        assertThat(source)
            .doesNotContain("ImportItemStatus.APPLIED")
            .doesNotContain("ImportItemStatus.NO_CHANGE")
            .doesNotContain("ImportItemStatus.FAILED")
            .doesNotContain("ImportItemStatus.REQUIRES_REVIEW");
    }
}
