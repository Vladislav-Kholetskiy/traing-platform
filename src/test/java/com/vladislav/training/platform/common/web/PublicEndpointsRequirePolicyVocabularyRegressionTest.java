package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code PublicEndpointsRequirePolicyVocabulary} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class PublicEndpointsRequirePolicyVocabularyRegressionTest {

    private static final Path ACCESS_READ_CONTOUR = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessReadArea.java"
    );
    private static final Path QUERY_CONTEXT_RESOLVER = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
    );
    private static final Path READ_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
    );
    private static final Path OPERATION_CODE = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityOperationCode.java"
    );
    private static final Path OPERATION_CODES = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityOperationCodes.java"
    );
    private static final Path TARGET_ENTITY_TYPE = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityTargetEntityType.java"
    );
    private static final Path REQUEST_FACTORY = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/CapabilityAdmissionRequestFactory.java"
    );
    private static final Path CANONICAL_ADMISSION_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/application/policy/DefaultCapabilityAdmissionPolicy.java"
    );
    private static final Path NOTIFICATION_ADMIN_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
    );
    private static final Path NOTIFICATION_SELF_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfReadController.java"
    );
    private static final Path IMPORT_ADMIN_COMMAND_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminCommandController.java"
    );
    private static final Path IMPORT_ADMIN_READ_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller/ImportAdminReadController.java"
    );
    private static final Path IMPORT_ITEM_REVIEW_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller/ImportItemReviewController.java"
    );
    private static final Path AUDIT_ADMIN_READ_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller/AuditAdminReadController.java"
    );
    private static final Path NOTIFICATION_ADMIN_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadServiceImpl.java"
    );
    private static final Path NOTIFICATION_SELF_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadServiceImpl.java"
    );
    private static final Path IMPORT_ADMIN_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportAdminReadServiceImpl.java"
    );
    private static final Path IMPORT_COMMAND_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportCommandServiceImpl.java"
    );
    private static final Path IMPORT_ITEM_REVIEW_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/service/ImportItemReviewServiceImpl.java"
    );
    private static final Path AUDIT_ADMIN_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );

    @Test
    void administrativeReadEndpointsMustHaveDedicatedReadContourVocabulary() throws Exception {
        String contours = read(ACCESS_READ_CONTOUR);
        String resolver = read(QUERY_CONTEXT_RESOLVER);
        String policy = read(READ_POLICY);
        String notificationAdminController = read(NOTIFICATION_ADMIN_CONTROLLER);
        String notificationSelfController = read(NOTIFICATION_SELF_CONTROLLER);
        String importReadController = read(IMPORT_ADMIN_READ_CONTROLLER);
        String auditReadController = read(AUDIT_ADMIN_READ_CONTROLLER);
        String notificationAdminReadService = read(NOTIFICATION_ADMIN_READ_SERVICE);
        String notificationSelfReadService = read(NOTIFICATION_SELF_READ_SERVICE);
        String importReadService = read(IMPORT_ADMIN_READ_SERVICE);
        String auditReadService = read(AUDIT_ADMIN_READ_SERVICE);

        assertThat(contours)
            .contains("NOTIFICATION_ADMINISTRATION")
            .contains("NOTIFICATION_RULE_ADMINISTRATION")
            .contains("NOTIFICATION_RECIPIENT_SELF")
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
            .contains("resolveAuditEventAdministrationDetailContext");

        assertThat(notificationAdminController).contains("/api/v1/admin/notifications");
        assertThat(notificationSelfController).contains("/api/v1/self/notifications");
        assertThat(importReadController)
            .contains("/import-jobs")
            .contains("/import-job-items/{itemId}");
        assertThat(auditReadController).contains("/api/v1/admin/audit-events");

        assertThat(notificationAdminReadService)
            .contains("NOTIFICATION_ADMINISTRATION")
            .contains("AccessSpecificationPolicy")
            .contains("AccessPolicyQueryContextResolver");
        assertThat(notificationSelfReadService)
            .contains("NOTIFICATION_RECIPIENT_SELF")
            .contains("AccessSpecificationPolicy")
            .contains("AccessPolicyQueryContextResolver");
        assertThat(importReadService)
            .contains("IMPORT_JOB_ADMINISTRATION")
            .contains("AccessSpecificationPolicy")
            .contains("AccessPolicyQueryContextResolver");
        assertThat(auditReadService)
            .contains("AUDIT_EVENT_ADMINISTRATION")
            .contains("AccessSpecificationPolicy")
            .contains("AccessPolicyQueryContextResolver");

        assertThat(policy)
            .contains("NOTIFICATION_RECIPIENT_SELF_TARGET_FAMILIES")
            .contains("NOTIFICATION_ADMINISTRATION_TARGET_FAMILIES")
            .contains("NOTIFICATION_RULE_ADMINISTRATION_TARGET_FAMILIES")
            .contains("IMPORT_JOB_ADMINISTRATION_TARGET_FAMILIES")
            .contains("AUDIT_EVENT_ADMINISTRATION_TARGET_FAMILIES");
    }

    @Test
    void administrativeMutationEndpointsMustHaveAdmissionVocabularyWhenRoutesExist() throws Exception {
        String operationCode = read(OPERATION_CODE);
        String operationCodes = read(OPERATION_CODES);
        String targetEntityType = read(TARGET_ENTITY_TYPE);
        String requestFactory = read(REQUEST_FACTORY);
        String canonicalPolicy = read(CANONICAL_ADMISSION_POLICY);
        String importCommandController = read(IMPORT_ADMIN_COMMAND_CONTROLLER);
        String importReviewController = read(IMPORT_ITEM_REVIEW_CONTROLLER);
        String importCommandService = read(IMPORT_COMMAND_SERVICE);
        String importReviewService = read(IMPORT_ITEM_REVIEW_SERVICE);

        assertThat(importCommandController).contains("@PostMapping");
        assertThat(importCommandService)
            .contains("CapabilityAdmissionPolicy")
            .contains("CapabilityAdmissionRequestFactory")
            .contains("createImportJobLaunch()");

        assertThat(operationCode)
            .contains("IMPORT_JOB_LAUNCH")
            .contains("IMPORT_ITEM_REVIEW_APPLY")
            .contains("IMPORT_ITEM_REVIEW_REJECT")
            .contains("NOTIFICATION_RULE_CREATE")
            .contains("NOTIFICATION_RULE_UPDATE")
            .contains("NOTIFICATION_RULE_ENABLE")
            .contains("NOTIFICATION_RULE_DISABLE");

        assertThat(operationCodes)
            .contains("IMPORT_JOB_LAUNCH")
            .contains("IMPORT_ITEM_REVIEW_APPLY")
            .contains("IMPORT_ITEM_REVIEW_REJECT")
            .contains("NOTIFICATION_RULE_CREATE")
            .contains("NOTIFICATION_RULE_UPDATE")
            .contains("NOTIFICATION_RULE_ENABLE")
            .contains("NOTIFICATION_RULE_DISABLE");

        assertThat(targetEntityType)
            .contains("IMPORT_JOB")
            .contains("IMPORT_JOB_ITEM")
            .contains("NOTIFICATION_RULE");

        assertThat(requestFactory)
            .contains("createImportJobLaunch")
            .contains("createImportItemReviewApply")
            .contains("createImportItemReviewReject")
            .contains("createNotificationRuleCreate")
            .contains("createNotificationRuleUpdate")
            .contains("createNotificationRuleEnable")
            .contains("createNotificationRuleDisable");

        assertThat(canonicalPolicy)
            .contains("case NOTIFICATION_RULE_CREATE")
            .contains("case NOTIFICATION_RULE_UPDATE")
            .contains("case NOTIFICATION_RULE_ENABLE")
            .contains("case NOTIFICATION_RULE_DISABLE")
            .contains("case IMPORT_JOB_LAUNCH");

        if (importReviewController.contains("/apply-review")) {
            assertThat(importReviewService)
                .contains("CapabilityAdmissionPolicy")
                .contains("CapabilityAdmissionRequestFactory")
                .contains("createImportItemReviewApply")
                .contains("createImportItemReviewReject");
        }
    }

    @Test
    void administrativeControllersMustNotBypassPolicyWithDirectRepositoriesOrJpaWrites() throws Exception {
        for (Path controller : List.of(
            NOTIFICATION_ADMIN_CONTROLLER,
            NOTIFICATION_SELF_CONTROLLER,
            IMPORT_ADMIN_COMMAND_CONTROLLER,
            IMPORT_ADMIN_READ_CONTROLLER,
            IMPORT_ITEM_REVIEW_CONTROLLER,
            AUDIT_ADMIN_READ_CONTROLLER
        )) {
            String source = read(controller);
            assertThat(source)
                
                .doesNotContain("JpaRepository")
                .doesNotContain("EntityManager")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("Repository")
                .doesNotContain(".save(")
                .doesNotContain(".delete(")
                .doesNotContain(".update(");
        }
    }

    @Test
    void optionalAdministrativeRoutesMustRemainAbsentRatherThanCountingAsImplicitGreen() throws Exception {
        String notificationAdminController = read(NOTIFICATION_ADMIN_CONTROLLER);
        String notificationSelfController = read(NOTIFICATION_SELF_CONTROLLER);
        String importReviewController = read(IMPORT_ITEM_REVIEW_CONTROLLER);
        String importCommandController = read(IMPORT_ADMIN_COMMAND_CONTROLLER);
        String importReadController = read(IMPORT_ADMIN_READ_CONTROLLER);
        String auditReadController = read(AUDIT_ADMIN_READ_CONTROLLER);

        assertThat(notificationAdminController)
            .doesNotContain("/retry")
            .doesNotContain("/suppress");
        assertThat(notificationSelfController)
            .doesNotContain("/retry")
            .doesNotContain("/suppress");
        assertThat(importCommandController)
            .doesNotContain("/retry")
            .doesNotContain("/suppress");
        assertThat(importReadController)
            .doesNotContain("/retry")
            .doesNotContain("/suppress");
        assertThat(importReviewController)
            .doesNotContain("/retry")
            .doesNotContain("/suppress");
        assertThat(auditReadController)
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}

