package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code NotificationAdminReadPolicy}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class NotificationAdminReadPolicyTest {

    private static final Path READ_SERVICE_INTERFACE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadService.java"
    );
    private static final Path READ_SERVICE_IMPLEMENTATION = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadServiceImpl.java"
    );

    @Test
    void adminNotificationReadFlowMustUseDedicatedAdministrativeReadContourBeforeMaterialization() {
        assertThat(Files.exists(READ_SERVICE_INTERFACE))
            
            .isTrue();
        assertThat(Files.exists(READ_SERVICE_IMPLEMENTATION))
            
            .isTrue();

        String interfaceSource = read(READ_SERVICE_INTERFACE);
        String implementationSource = read(READ_SERVICE_IMPLEMENTATION);

        assertThat(interfaceSource)
            .contains("listAdminNotifications")
            .contains("findAdminNotificationById");

        assertThat(implementationSource)
            .contains("AccessPolicyQueryContextResolver")
            .contains("AccessSpecificationPolicy")
            .contains("NotificationQueryService")
            .contains("resolveNotificationAdministrationContext")
            .contains("resolveNotificationAdministrationDetailContext")
            .contains("AccessReadArea.NOTIFICATION_ADMINISTRATION")
            .doesNotContain("AccessReadArea.SELF_RESULT_HISTORY")
            .doesNotContain("AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION")
            .doesNotContain("AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS")
            .doesNotContain("AccessReadArea.EXPERT_QUESTION_ANALYTICS")
            .doesNotContain("NotificationRepository");

        assertThat(implementationSource.indexOf("accessSpecificationPolicy"))
            
            .isGreaterThanOrEqualTo(0);
        assertThat(implementationSource.indexOf("notificationQueryService"))
            
            .isGreaterThanOrEqualTo(0);
        assertThat(implementationSource.indexOf("accessSpecificationPolicy"))
            .isLessThan(implementationSource.indexOf("notificationQueryService"));
    }

    @Test
    void denyPathMustNotMaterializeNotificationDtosOrReuseAnalyticsContours() {
        assertThat(Files.exists(READ_SERVICE_IMPLEMENTATION))
            
            .isTrue();

        String implementationSource = read(READ_SERVICE_IMPLEMENTATION);

        assertThat(implementationSource)
            .contains("PolicyViolationException")
            .doesNotContain("SELF_RESULT_HISTORY")
            .doesNotContain("MANAGERIAL_CURRENT_SUPERVISION")
            .doesNotContain("MANAGERIAL_HISTORICAL_ANALYTICS")
            .doesNotContain("EXPERT_QUESTION_ANALYTICS");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}
