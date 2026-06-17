package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code NotificationDoesNotMutateOwnerFacts} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class NotificationDoesNotMutateOwnerFactsRegressionTest {

    @Test
    void notificationServiceAndPersistencePackagesStayOwnerDecoupledAndControllerFree() throws Exception {
        assertPackageIsOwnerDecoupled(Path.of("src/main/java/com/vladislav/training/platform/notification/service"));
        assertPackageIsOwnerDecoupled(
            Path.of("src/main/java/com/vladislav/training/platform/notification/infrastructure/persistence")
        );
        assertControllerPackageStaysOwnerDecoupled();
    }

    @Test
    void dispatchAndRuntimeCoreServicesStayNotificationLocal() throws Exception {
        assertNotificationLocal(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationDispatchServiceImpl.java"
        ));
        assertNotificationLocal(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"
        ));
        assertNotificationLocal(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        Path queryService = Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"
        );
        assertNotificationLocal(queryService);
        assertThat(Files.readString(queryService)).doesNotContain(".save(");
        assertThat(Files.readString(queryService)).doesNotContain(".update(");
    }

    private void assertPackageIsOwnerDecoupled(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                assertOwnerDecoupled(Files.readString(file));
            }
        }
    }

    private void assertNotificationLocal(Path sourcePath) throws Exception {
        String source = Files.readString(sourcePath);
        assertOwnerDecoupled(source);

        assertThat(source).doesNotContain("EmailNotificationProvider");
        assertThat(source).doesNotContain("SmsNotificationProvider");
        assertThat(source).doesNotContain("TelegramNotificationProvider");
        assertThat(source).doesNotContain("NotificationProviderRegistry");
        assertThat(source).doesNotContain("NotificationScheduler");
        assertThat(source).doesNotContain("DeadlineReminderScheduler");
        assertThat(source).doesNotContain("@RestController");
        assertThat(source).doesNotContain("@Controller");
        assertThat(source).doesNotContain("@RequestMapping");
        assertThat(source).doesNotContain("@GetMapping");
        assertThat(source).doesNotContain("@PostMapping");
        assertThat(source).doesNotContain("@PutMapping");
        assertThat(source).doesNotContain("@PatchMapping");
        assertThat(source).doesNotContain("@DeleteMapping");
    }

    private void assertOwnerDecoupled(String source) {
        assertThat(source).doesNotContain("AssignmentEntity");
        assertThat(source).doesNotContain("TestAttemptEntity");
        assertThat(source).doesNotContain("ResultEntity");
        assertThat(source).doesNotContain("CourseEntity");
        assertThat(source).doesNotContain("UserEntity");
        assertThat(source).doesNotContain("UserOrganizationEntity");
        assertThat(source).doesNotContain("OrganizationUnitEntity");
        assertThat(source).doesNotContain("OrganizationalUnitEntity");

        assertThat(source).doesNotContain("AssignmentRepository");
        assertThat(source).doesNotContain("TestAttemptRepository");
        assertThat(source).doesNotContain("ResultRepository");
        assertThat(source).doesNotContain("CourseRepository");
        assertThat(source).doesNotContain("UserRepository");
        assertThat(source).doesNotContain("OrganizationUnitRepository");
        assertThat(source).doesNotContain("OrganizationalUnitRepository");

        assertThat(source).doesNotContain("saveAssignment");
        assertThat(source).doesNotContain("updateAssignment");
        assertThat(source).doesNotContain("recalculateAssignment");
        assertThat(source).doesNotContain("closeAssignment");
        assertThat(source).doesNotContain("terminalizeAttempt");
        assertThat(source).doesNotContain("submitAttempt");
        assertThat(source).doesNotContain("recordResult");
        assertThat(source).doesNotContain("rebuildResult");
        assertThat(source).doesNotContain("publishCourse");
        assertThat(source).doesNotContain("archiveCourse");
        assertThat(source).doesNotContain("assignRole");
        assertThat(source).doesNotContain("revokeRole");
        assertThat(source).doesNotContain("patchOwner");
        assertThat(source).doesNotContain("ownerTable");
        assertThat(source).doesNotContain("databaseTable");
    }

    private void assertControllerPackageStaysOwnerDecoupled() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                String fileName = file.getFileName().toString();
                boolean lifecycleCommandController = fileName.equals("NotificationAdminCommandController.java")
                    || fileName.equals("NotificationSelfCommandController.java");
                assertOwnerDecoupled(source);
                assertThat(source).doesNotContain("SpringDataNotificationJpaRepository");
                assertThat(source).doesNotContain("SpringDataNotificationRuleJpaRepository");
                assertThat(source).doesNotContain("JpaNotificationRepositoryAdapter");
                assertThat(source).doesNotContain("JpaNotificationRuleRepositoryAdapter");
                assertThat(source).doesNotContain("NotificationDispatchService");
                assertThat(source).doesNotContain("NotificationRuleService");
                assertThat(source).doesNotContain(".save(");
                assertThat(source).doesNotContain(".update(");
                assertThat(source).doesNotContain(".delete(");
                if (!lifecycleCommandController) {
                    assertThat(source).doesNotContain("@PostMapping");
                }
                assertThat(source).doesNotContain("@PutMapping");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
            }
        }
    }
}
