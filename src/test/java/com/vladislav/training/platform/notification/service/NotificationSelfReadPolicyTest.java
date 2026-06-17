package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code NotificationSelfReadPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class NotificationSelfReadPolicyTest {

    private static final Path ACCESS_READ_CONTOUR = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessReadArea.java"
    );
    private static final Path QUERY_CONTEXT_RESOLVER = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
    );
    private static final Path ACCESS_SPEC_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
    );
    private static final Path QUERY_SERVICE_INTERFACE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryService.java"
    );
    private static final Path SELF_READ_SERVICE_INTERFACE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadService.java"
    );
    private static final Path SELF_READ_SERVICE_IMPLEMENTATION = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadServiceImpl.java"
    );

    @Test
    void selfNotificationReadFlowMustUseDedicatedAdministrativeSelfContourAndScopedDetailLookup() {
        assertThat(read(ACCESS_READ_CONTOUR))
            .contains("NOTIFICATION_RECIPIENT_SELF");
        assertThat(read(QUERY_CONTEXT_RESOLVER))
            .contains("resolveNotificationRecipientSelfContext")
            .contains("resolveNotificationRecipientSelfDetailContext")
            .doesNotContain("resolveNotificationRecipientSelfContext(Long actorUserId, Long recipientUserId)");
        assertThat(read(ACCESS_SPEC_POLICY))
            .contains("NOTIFICATION_RECIPIENT_SELF")
            .contains("self_notification")
            .doesNotContain("NOTIFICATION_ADMINISTRATION_TARGET_FAMILIES = Set.of(\"self_notification\")");
        assertThat(read(QUERY_SERVICE_INTERFACE))
            .contains("findNotificationByIdAndRecipientUserId");
        assertThat(Files.exists(SELF_READ_SERVICE_INTERFACE))
            
            .isTrue();
        assertThat(Files.exists(SELF_READ_SERVICE_IMPLEMENTATION))
            
            .isTrue();

        String interfaceSource = read(SELF_READ_SERVICE_INTERFACE);
        String implementationSource = read(SELF_READ_SERVICE_IMPLEMENTATION);

        assertThat(interfaceSource)
            .contains("listSelfNotifications")
            .contains("findSelfNotificationById");
        assertThat(implementationSource)
            .contains("resolveNotificationRecipientSelfContext")
            .contains("resolveNotificationRecipientSelfDetailContext")
            .contains("AccessReadArea.NOTIFICATION_RECIPIENT_SELF")
            .contains("findNotificationsByRecipientUserId")
            .contains("findNotificationByIdAndRecipientUserId")
            .doesNotContain("AccessReadArea.NOTIFICATION_ADMINISTRATION")
            .doesNotContain("NotificationRepository");

        assertThat(implementationSource.indexOf("accessSpecificationPolicy"))
            .isLessThan(implementationSource.indexOf("notificationQueryService"));
    }

    @Test
    void selfNotificationReadDenyPathMustHappenBeforeMaterializationAndMustNotGrantCrossRecipientAccess() {
        assertThat(Files.exists(SELF_READ_SERVICE_IMPLEMENTATION))
            
            .isTrue();

        String implementationSource = read(SELF_READ_SERVICE_IMPLEMENTATION);

        assertThat(implementationSource)
            .contains("PolicyViolationException")
            .doesNotContain("NOTIFICATION_ADMINISTRATION")
            .doesNotContain("SELF_RESULT_HISTORY")
            .doesNotContain("MANAGERIAL_CURRENT_SUPERVISION")
            .doesNotContain("MANAGERIAL_HISTORICAL_ANALYTICS")
            .doesNotContain("EXPERT_QUESTION_ANALYTICS")
            .doesNotContain("findNotificationById(");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
