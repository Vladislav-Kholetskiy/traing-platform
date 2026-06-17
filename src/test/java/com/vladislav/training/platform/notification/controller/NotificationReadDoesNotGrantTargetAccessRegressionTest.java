package com.vladislav.training.platform.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code NotificationReadDoesNotGrantTargetAccess} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class NotificationReadDoesNotGrantTargetAccessRegressionTest {

    private static final Path RESPONSE_DTO_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/dto/NotificationAdminReadResponse.java"
    );
    private static final Path SELF_RESPONSE_DTO_PATH = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/dto/NotificationSelfReadResponse.java"
    );

    @Test
    void notificationAdminReadDtoExposesOnlyDeliveryStateFieldsAndReferenceLinkage() {
        assertThat(Files.exists(RESPONSE_DTO_PATH))
            
            .isTrue();

        String source = read(RESPONSE_DTO_PATH);

        assertThat(source)
            .contains("Long id")
            .contains("Long recipientUserId")
            .contains("String notificationType")
            .contains("NotificationChannel channelCode")
            .contains("NotificationStatus status")
            .contains("String sourceEntityType")
            .contains("String sourceEntityId")
            .contains("Instant scheduledAt")
            .contains("Instant sentAt")
            .contains("int deliveryAttemptCount")
            .contains("String errorCode")
            .contains("Instant createdAt")
            .contains("Instant updatedAt")
            .doesNotContain("payloadSnapshot")
            .doesNotContain("Assignment")
            .doesNotContain("TestAttempt")
            .doesNotContain("Result")
            .doesNotContain("Course")
            .doesNotContain("User ")
            .doesNotContain("targetObject")
            .doesNotContain("permission")
            .doesNotContain("authorization");
    }

    @Test
    void notificationReadSurfaceMustNotBecomeImplicitPermissionToReadTargetDomainObjects() {
        String notificationControllerSource = Files.exists(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
        ))
            ? read(Path.of(
                "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
            ))
            : "";

        assertThat(notificationControllerSource)
            .doesNotContain("AssignmentService")
            .doesNotContain("TestAttempt")
            .doesNotContain("Result")
            .doesNotContain("Course")
            .doesNotContain("UserRepository")
            .doesNotContain("permissionVerdict");
    }

    @Test
    void notificationSelfReadDtoExposesOnlyUserFacingInboxFieldsWithoutDeliveryInternals() {
        assertThat(Files.exists(SELF_RESPONSE_DTO_PATH))
            
            .isTrue();

        String source = read(SELF_RESPONSE_DTO_PATH);

        assertThat(source)
            .contains("Long id")
            .contains("String title")
            .contains("String message")
            .contains("NotificationChannel channelCode")
            .contains("Instant createdAt")
            .contains("String notificationType")
            .contains("String companyName")
            .contains("java.util.List<NotificationAssignmentRecipientResponse> assignmentRecipients")
            .contains("String courseName")
            .doesNotContain("String sourceEntityType")
            .doesNotContain("String sourceEntityId")
            .doesNotContain("NotificationStatus status")
            .doesNotContain("Instant scheduledAt")
            .doesNotContain("Instant sentAt")
            .doesNotContain("int deliveryAttemptCount")
            .doesNotContain("String errorCode")
            .doesNotContain("String errorMessage")
            .doesNotContain("Instant updatedAt")
            .doesNotContain("payloadSnapshot")
            .doesNotContain("TestAttempt")
            .doesNotContain("Result")
            .doesNotContain("User ")
            .doesNotContain("targetObject")
            .doesNotContain("permission")
            .doesNotContain("authorization");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
