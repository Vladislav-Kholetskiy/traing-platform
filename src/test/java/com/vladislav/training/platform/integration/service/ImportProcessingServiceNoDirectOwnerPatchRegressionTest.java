package com.vladislav.training.platform.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code ImportProcessingServiceNoDirectOwnerPatch} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class ImportProcessingServiceNoDirectOwnerPatchRegressionTest {

    private static final Path IMPORT_PROCESSING_SERVICE_IMPL = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportProcessingServiceImpl.java"
    );
    private static final Path MAIN_SRC = Path.of("src/main/java");

    @Test
    void processingImplMustStayInsideImportContourAndAvoidDirectOwnerPatches() throws Exception {
        String source = Files.readString(IMPORT_PROCESSING_SERVICE_IMPL);

        assertThat(source)
            .contains("class ImportProcessingServiceImpl implements ImportProcessingService")
            .contains("UserCommandService")
            .contains("appUserRepository.findAllUsers()")
            .doesNotContain("appUserRepository.saveUser(")
            .doesNotContain("appUserRepository.save(")
            .doesNotContain("appUserRepository.delete(")
            .doesNotContain("appUserRepository.update(")
            .doesNotContain("EntityManager")
            .doesNotContain("JdbcTemplate")
            .doesNotContain("createNativeQuery(")
            .doesNotContain("executeUpdate(")
            .doesNotContain("insert into app_user")
            .doesNotContain("update app_user")
            .doesNotContain("delete from app_user")
            .doesNotContain("UserOrganizationAssignmentRepository")
            .doesNotContain("UserRoleAssignmentRepository")
            .doesNotContain("AssignmentRepository")
            .doesNotContain("ResultRepository")
            .doesNotContain("TestAttemptRepository")
            .doesNotContain("CourseRepository")
            .doesNotContain("NotificationRepository")
            .doesNotContain("@RestController")
            .doesNotContain("@RequestMapping")
            .doesNotContain("@Scheduled")
            .doesNotContain("Scheduler")
            .doesNotContain("recordAudit(")
            .doesNotContain("reconcile")
            .doesNotContain("backfill");
    }

    @Test
    void processingContourMustStaySeparateFromTypedLaunchReadAndReviewControllers() throws Exception {
        try (java.util.stream.Stream<Path> stream = Files.walk(
            Path.of("src/main/java/com/vladislav/training/platform/integration/controller")
        )) {
            List<String> importControllers = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.getFileName().toString().contains("Import"))
                .filter(path -> path.getFileName().toString().contains("Controller"))
                .map(Path::toString)
                .toList();

            assertThat(importControllers)
                .contains("src\\main\\java\\com\\vladislav\\training\\platform\\integration\\controller\\ImportAdminCommandController.java")
                .contains("src\\main\\java\\com\\vladislav\\training\\platform\\integration\\controller\\ImportAdminReadController.java")
                .contains("src\\main\\java\\com\\vladislav\\training\\platform\\integration\\controller\\ImportItemReviewController.java")
                .hasSize(3);
        }

        assertThat(Files.exists(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/controller/PersonnelExcelImportController.java"
        )))
            
            .isTrue();
    }
}
