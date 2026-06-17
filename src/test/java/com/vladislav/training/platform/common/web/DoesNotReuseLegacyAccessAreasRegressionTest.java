package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code DoesNotReuseLegacyAccessAreas} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class DoesNotReuseLegacyAccessAreasRegressionTest {

    private static final String SELF_RESULT_HISTORY = "SELF_RESULT_HISTORY";
    private static final String MANAGERIAL_CURRENT_SUPERVISION = "MANAGERIAL_CURRENT_SUPERVISION";
    private static final String MANAGERIAL_HISTORICAL_ANALYTICS = "MANAGERIAL_HISTORICAL_ANALYTICS";
    private static final String EXPERT_QUESTION_ANALYTICS = "EXPERT_QUESTION_ANALYTICS";

    @Test
    void accessReadContourAndResolverMustExposeDedicatedAdministrativeReadContours() throws Exception {
        String contours = read("src/main/java/com/vladislav/training/platform/access/service/AccessReadArea.java");
        String resolver = read(
            "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
        );

        assertThat(contours)
            .contains("NOTIFICATION_ADMINISTRATION")
            .contains("NOTIFICATION_RECIPIENT_SELF")
            .contains("NOTIFICATION_RULE_ADMINISTRATION")
            .contains("IMPORT_JOB_ADMINISTRATION")
            .contains("AUDIT_EVENT_ADMINISTRATION");

        assertThat(resolver)
            .contains("resolveNotificationAdministrationContext")
            .contains("resolveNotificationAdministrationDetailContext")
            .contains("resolveNotificationRecipientSelfContext")
            .contains("resolveNotificationRecipientSelfDetailContext")
            .contains("resolveNotificationRuleAdministrationContext")
            .contains("resolveNotificationRuleAdministrationDetailContext")
            .contains("resolveImportJobAdministrationContext")
            .contains("resolveImportJobAdministrationDetailContext")
            .contains("resolveImportJobItemAdministrationContext")
            .contains("resolveImportJobItemAdministrationDetailContext")
            .contains("resolveAuditEventAdministrationContext")
            .contains("resolveAuditEventAdministrationDetailContext")
            .doesNotContain("resolveNotificationAdministrationContext(Long actorUserId, AccessReadArea.SELF_RESULT_HISTORY)")
            .doesNotContain("resolveNotificationRecipientSelfContext(Long actorUserId, AccessReadArea.SELF_RESULT_HISTORY)");
    }

    @Test
    void administrativeReadServicesAndControllersMustNotReferenceAnalyticsContours() throws Exception {
        String notificationAdminReadService = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadServiceImpl.java"
        );
        String notificationSelfReadService = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadServiceImpl.java"
        );
        String importAdminReadService = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportAdminReadServiceImpl.java"
        );
        String auditAdminReadService = read(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
        );
        String notificationAdminController = read(
            "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
        );
        String notificationSelfController = read(
            "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfReadController.java"
        );
        String importReadController = read(
            "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminReadController.java"
        );
        String auditReadController = read(
            "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
        );

        assertAdministrativeReadPathUsesNoAnalyticsContour(notificationAdminReadService);
        assertAdministrativeReadPathUsesNoAnalyticsContour(notificationSelfReadService);
        assertAdministrativeReadPathUsesNoAnalyticsContour(importAdminReadService);
        assertAdministrativeReadPathUsesNoAnalyticsContour(auditAdminReadService);
        assertAdministrativeReadPathUsesNoAnalyticsContour(notificationAdminController);
        assertAdministrativeReadPathUsesNoAnalyticsContour(notificationSelfController);
        assertAdministrativeReadPathUsesNoAnalyticsContour(importReadController);
        assertAdministrativeReadPathUsesNoAnalyticsContour(auditReadController);

        assertThat(notificationAdminReadService).contains("NOTIFICATION_ADMINISTRATION");
        assertThat(notificationSelfReadService).contains("NOTIFICATION_RECIPIENT_SELF");
        assertThat(importAdminReadService).contains("IMPORT_JOB_ADMINISTRATION");
        assertThat(auditAdminReadService)
            .contains("AUDIT_EVENT_ADMINISTRATION")
            .doesNotContain("CapabilityAdmissionPolicy");
    }

    @Test
    void jpaAccessSpecificationPolicyMustKeepAdministrativeReadsOnDedicatedBranches() throws Exception {
        String policy = read("src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java");

        assertThat(policy)
            .contains("NOTIFICATION_RECIPIENT_SELF_TARGET_FAMILIES")
            .contains("NOTIFICATION_ADMINISTRATION_TARGET_FAMILIES")
            .contains("NOTIFICATION_RULE_ADMINISTRATION_TARGET_FAMILIES")
            .contains("IMPORT_JOB_ADMINISTRATION_TARGET_FAMILIES")
            .contains("AUDIT_EVENT_ADMINISTRATION_TARGET_FAMILIES")
            .contains("isNotificationRecipientSelfContour")
            .contains("isAdministrativeAdministrativeContour")
            .contains("isKnownNotificationRecipientSelfReadContext")
            .contains("isKnownAdministrativeAdministrativeReadContext");

        assertThat(policy)
            .doesNotContain("AccessReadArea.NOTIFICATION_RECIPIENT_SELF,\r\n            AccessReadType.LIST,\r\n            effectiveAt,\r\n            null,\r\n            null,\r\n            \"self_result_history\"")
            .doesNotContain("AccessReadArea.NOTIFICATION_ADMINISTRATION,\r\n            AccessReadType")
            .doesNotContain("AccessReadArea.IMPORT_JOB_ADMINISTRATION,\r\n            AccessReadType.ANALYTICS")
            .doesNotContain("AccessReadArea.AUDIT_EVENT_ADMINISTRATION,\r\n            AccessReadType.ANALYTICS");
    }

    @Test
    void administrativeReadQueryImplementationsMustNotCarryAnalyticsContourFallbacks() throws Exception {
        String notificationQuery = read(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"
        );
        String importQuery = read(
            "src/main/java/com/vladislav/training/platform/integration/service/ImportQueryServiceImpl.java"
        );
        String auditQuery = read(
            "src/main/java/com/vladislav/training/platform/audit/service/AuditQueryServiceImpl.java"
        );

        assertAdministrativeReadPathUsesNoAnalyticsContour(notificationQuery);
        assertAdministrativeReadPathUsesNoAnalyticsContour(importQuery);
        assertAdministrativeReadPathUsesNoAnalyticsContour(auditQuery);
    }

    private void assertAdministrativeReadPathUsesNoAnalyticsContour(String source) {
        assertThat(source)
            .doesNotContain(SELF_RESULT_HISTORY)
            .doesNotContain(MANAGERIAL_CURRENT_SUPERVISION)
            .doesNotContain(MANAGERIAL_HISTORICAL_ANALYTICS)
            .doesNotContain(EXPERT_QUESTION_ANALYTICS);
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
