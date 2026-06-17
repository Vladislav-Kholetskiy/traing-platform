package com.vladislav.training.platform.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение контроллера {@code Notification}.
 * Основное внимание здесь уделено адресам API и ответам.
 */
class NotificationControllerTest {

    private static final Path CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
    );
    private static final Path SELF_CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfReadController.java"
    );
    private static final Path ADMIN_COMMAND_CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminCommandController.java"
    );
    private static final Path SELF_COMMAND_CONTROLLER_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfCommandController.java"
    );

    @Test
    void adminNotificationControllerPublishesOnlyDedicatedGetListAndDetailRoutes() {
        assertThat(Files.exists(CONTROLLER_PATH))
            
            .isTrue();

        String source = read(CONTROLLER_PATH);

        assertThat(source)
            .contains("@RestController")
            .contains("@RequestMapping(\"/api/v1/admin/notifications\")")
            .contains("@GetMapping")
            .contains("@GetMapping(\"/{notificationId}\")")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("NotificationRepository")
            .doesNotContain("NotificationEntity");
    }

    @Test
    void controllerResolvesActorFromTrustedInteractiveContextAndReturnsDtoReadModel() {
        assertThat(Files.exists(CONTROLLER_PATH))
            
            .isTrue();

        String source = read(CONTROLLER_PATH);

        assertThat(source)
            .contains("InteractiveActorResolver")
            .contains("NotificationAdminReadResponse")
            .doesNotContain("@RequestParam(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"recipientUserId\")")
            .doesNotContain("@RequestParam(\"scope\")")
            .doesNotContain("@RequestParam(\"scopeOverride\")")
            .doesNotContain("@RequestParam(\"targetUserId\")")
            .doesNotContain("@RequestParam(\"managerUserId\")");
    }

    @Test
    void selfNotificationControllerPublishesOnlyDedicatedGetListAndDetailRoutes() {
        assertThat(Files.exists(SELF_CONTROLLER_PATH))
            
            .isTrue();

        String source = read(SELF_CONTROLLER_PATH);

        assertThat(source)
            .contains("@RestController")
            .contains("@RequestMapping(\"/api/v1/self/notifications\")")
            .contains("@GetMapping")
            .contains("@GetMapping(\"/{notificationId}\")")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("NotificationRepository")
            .doesNotContain("NotificationEntity");
    }

    @Test
    void selfControllerResolvesActorFromTrustedInteractiveContextWithoutRecipientSelectors() {
        assertThat(Files.exists(SELF_CONTROLLER_PATH))
            
            .isTrue();

        String source = read(SELF_CONTROLLER_PATH);

        assertThat(source)
            .contains("InteractiveActorResolver")
            .contains("NotificationSelfReadResponse")
            .doesNotContain("@RequestParam(\"actorUserId\")")
            .doesNotContain("@RequestParam(\"recipientUserId\")")
            .doesNotContain("@RequestParam(\"userId\")")
            .doesNotContain("@RequestParam(\"scope\")")
            .doesNotContain("@RequestParam(\"scopeOverride\")")
            .doesNotContain("@RequestParam(\"targetUserId\")")
            .doesNotContain("@RequestParam(\"managerUserId\")");
    }

    @Test
    void adminNotificationCommandControllerPublishesDispatchPendingRouteOnly() {
        assertThat(Files.exists(ADMIN_COMMAND_CONTROLLER_PATH)).isTrue();

        String source = read(ADMIN_COMMAND_CONTROLLER_PATH);

        assertThat(source)
            .contains("@RequestMapping(\"/api/v1/admin/notifications\")")
            .contains("@PostMapping(\"/dispatch-pending\")")
            .contains("NotificationAdminDispatchService")
            .contains("InteractiveActorResolver")
            .doesNotContain("NotificationRepository")
            .doesNotContain("NotificationEntity")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");
    }

    @Test
    void selfNotificationCommandControllerPublishesReadLifecycleRoutesOnly() {
        assertThat(Files.exists(SELF_COMMAND_CONTROLLER_PATH)).isTrue();

        String source = read(SELF_COMMAND_CONTROLLER_PATH);

        assertThat(source)
            .contains("@RequestMapping(\"/api/v1/self/notifications\")")
            .contains("@PostMapping(\"/{notificationId}/read\")")
            .contains("@PostMapping(\"/read-all\")")
            .contains("NotificationSelfCommandService")
            .contains("InteractiveActorResolver")
            .doesNotContain("NotificationRepository")
            .doesNotContain("NotificationEntity")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
