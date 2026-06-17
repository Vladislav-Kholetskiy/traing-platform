package com.vladislav.training.platform.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code NotificationApiPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class NotificationApiPerimeterRegressionTest {

    private static final Path NOTIFICATION_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
    );
    private static final Path SELF_NOTIFICATION_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfReadController.java"
    );
    private static final Path ADMIN_NOTIFICATION_COMMAND_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminCommandController.java"
    );
    private static final Path SELF_NOTIFICATION_COMMAND_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfCommandController.java"
    );

    @Test
    void notificationApiSurfacePublishesDedicatedReadAndLifecycleRoutesOnly() throws IOException {
        List<Path> controllerSources = productionNotificationControllerSources();

        assertThat(controllerSources)
            
            .contains(NOTIFICATION_CONTROLLER);
        assertThat(controllerSources)
            
            .contains(SELF_NOTIFICATION_CONTROLLER);
        assertThat(controllerSources)
            .contains(ADMIN_NOTIFICATION_COMMAND_CONTROLLER, SELF_NOTIFICATION_COMMAND_CONTROLLER);

        String dedicatedControllerSource = read(NOTIFICATION_CONTROLLER);
        assertThat(dedicatedControllerSource)
            .contains("/api/v1/admin/notifications")
            .doesNotContain("/api/v1/admin/operational/")
            .doesNotContain("/api/v1/admin/tables/")
            .doesNotContain("/retry")
            .doesNotContain("/suppress")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");

        String selfControllerSource = read(SELF_NOTIFICATION_CONTROLLER);
        assertThat(selfControllerSource)
            .contains("/api/v1/self/notifications")
            .doesNotContain("/retry")
            .doesNotContain("/suppress")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");

        String adminCommandControllerSource = read(ADMIN_NOTIFICATION_COMMAND_CONTROLLER);
        assertThat(adminCommandControllerSource)
            .contains("@PostMapping(\"/dispatch-pending\")")
            .doesNotContain("/retry")
            .doesNotContain("/suppress")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");

        String selfCommandControllerSource = read(SELF_NOTIFICATION_COMMAND_CONTROLLER);
        assertThat(selfCommandControllerSource)
            .contains("@PostMapping(\"/{notificationId}/read\")")
            .contains("@PostMapping(\"/read-all\")")
            .doesNotContain("/retry")
            .doesNotContain("/suppress")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");

        for (Path controllerSource : controllerSources) {
            String source = read(controllerSource);
            assertThat(source)
                
                .doesNotContain("/api/v1/admin/operational/")
                .doesNotContain("/api/v1/admin/tables/")
                .doesNotContain("GenericNotificationController")
                .doesNotContain("NotificationCrudController");
        }
    }

    private List<Path> productionNotificationControllerSources() throws IOException {
        Path root = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
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
