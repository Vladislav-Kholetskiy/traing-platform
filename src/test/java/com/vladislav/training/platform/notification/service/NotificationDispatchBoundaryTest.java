package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет граничные случаи для {@code NotificationDispatch}.
 * Такие сценарии особенно важны на границах допустимого поведения.
 */
class NotificationDispatchBoundaryTest {

    @Test
    void dispatchImplementationMustExistAsDedicatedBoundaryService() throws Exception {
        Class<?> dispatchImpl = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationDispatchServiceImpl"
        );

        assertThat(NotificationDispatchService.class).isAssignableFrom(dispatchImpl);
        assertThat(hasConstructorDependency(dispatchImpl, NotificationDispatchService.class)).isFalse();

        String dispatchSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationDispatchServiceImpl.java"
        ));

        assertBoundarySafe(dispatchSource);
        assertThat(dispatchSource).doesNotContain("Controller");
        assertThat(dispatchSource).doesNotContain("Scheduler");
        assertThat(dispatchSource).doesNotContain("EmailNotificationProvider");
        assertThat(dispatchSource).doesNotContain("SmsNotificationProvider");
        assertThat(dispatchSource).doesNotContain("TelegramNotificationProvider");
        assertThat(dispatchSource).doesNotContain("NotificationProviderRegistry");
    }

    @Test
    void existingNotificationRuntimeCoreServicesMustStayDispatchFreeAndControllerFree() throws Exception {
        String commandSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"
        ));
        String querySource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"
        ));
        String ruleSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        assertBoundarySafe(commandSource);
        assertBoundarySafe(querySource);
        assertBoundarySafe(ruleSource);

        assertThat(commandSource).doesNotContain("NotificationDispatchService");
        assertThat(querySource).doesNotContain("NotificationDispatchService");
        assertThat(ruleSource).doesNotContain("NotificationDispatchService");
        assertThat(querySource).doesNotContain(".save(");
        assertThat(ruleSource).doesNotContain("NotificationRepository");
        assertControllerBoundaryRemainsDispatchFree();
    }

    @Test
    void notificationServicePackageMustStayProviderAndSchedulerFreeAtStageTwoPointSix() throws Exception {
        Path serviceRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/service");

        try (var files = Files.walk(serviceRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertBoundarySafe(source);
                assertThat(source).doesNotContain("NotificationScheduler");
                assertThat(source).doesNotContain("DeadlineReminderScheduler");
                assertThat(source).doesNotContain("EmailNotificationProvider");
                assertThat(source).doesNotContain("SmsNotificationProvider");
                assertThat(source).doesNotContain("TelegramNotificationProvider");
                assertThat(source).doesNotContain("NotificationProviderRegistry");
            }
        }
    }

    private void assertBoundarySafe(String source) {
        assertThat(source).doesNotContain("AssignmentEntity");
        assertThat(source).doesNotContain("TestAttemptEntity");
        assertThat(source).doesNotContain("ResultEntity");
        assertThat(source).doesNotContain("CourseEntity");
        assertThat(source).doesNotContain("UserEntity");
        assertThat(source).doesNotContain("AssignmentRepository");
        assertThat(source).doesNotContain("TestAttemptRepository");
        assertThat(source).doesNotContain("ResultRepository");
        assertThat(source).doesNotContain("CourseRepository");
    }

    private boolean hasConstructorDependency(Class<?> type, Class<?> dependencyType) {
        return Arrays.stream(type.getDeclaredConstructors())
            .map(Constructor::getParameterTypes)
            .flatMap(Arrays::stream)
            .anyMatch(parameterType -> parameterType.equals(dependencyType));
    }

    private void assertControllerBoundaryRemainsDispatchFree() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists().isDirectory();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                String fileName = file.getFileName().toString();
                boolean commandController = fileName.equals("NotificationAdminCommandController.java")
                    || fileName.equals("NotificationSelfCommandController.java");
                assertThat(source).doesNotContain("NotificationDispatchService");
                assertThat(source).doesNotContain("NotificationDeliveryGateway");
                assertThat(source).doesNotContain("NotificationProviderRegistry");
                if (!commandController) {
                    assertThat(source).doesNotContain("@PostMapping");
                }
                assertThat(source).doesNotContain("@PutMapping");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
            }
        }
    }
}
