package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code NotificationReceiptDoesNotGrantTargetAccess} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class NotificationReceiptDoesNotGrantTargetAccessRegressionTest {

    private static final Path NOTIFICATION_ADMIN_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationAdminReadController.java"
    );
    private static final Path NOTIFICATION_SELF_CONTROLLER = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/NotificationSelfReadController.java"
    );
    private static final Path NOTIFICATION_ADMIN_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationAdminReadServiceImpl.java"
    );
    private static final Path NOTIFICATION_SELF_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationSelfReadServiceImpl.java"
    );
    private static final Path NOTIFICATION_QUERY_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"
    );
    private static final Path NOTIFICATION_ADMIN_RESPONSE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/dto/NotificationAdminReadResponse.java"
    );
    private static final Path NOTIFICATION_SELF_RESPONSE = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller/dto/NotificationSelfReadResponse.java"
    );
    private static final Path ACCESS_SPECIFICATION_POLICY = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/JpaAccessSpecificationPolicy.java"
    );
    private static final Path QUERY_CONTEXT_RESOLVER = Path.of(
        "src/main/java/com/vladislav/training/platform/access/service/AccessPolicyQueryContextResolver.java"
    );

    @Test
    void notificationReadPathMustNotCallTargetDomainDetailServices() throws Exception {
        for (Path sourceFile : List.of(
            NOTIFICATION_ADMIN_CONTROLLER,
            NOTIFICATION_SELF_CONTROLLER,
            NOTIFICATION_ADMIN_READ_SERVICE,
            NOTIFICATION_SELF_READ_SERVICE,
            NOTIFICATION_QUERY_SERVICE
        )) {
            String source = read(sourceFile);
            assertThat(source)
                
                .doesNotContain("AssignmentQueryService")
                .doesNotContain("AssignmentCampaignQueryService")
                .doesNotContain("AssignmentSelfScopedQueryService")
                .doesNotContain("CourseQueryService")
                .doesNotContain("MaterialQueryService")
                .doesNotContain("QuestionQueryService")
                .doesNotContain("TestQueryService")
                .doesNotContain("TopicQueryService")
                .doesNotContain("ContentLifecycleQueryService")
                .doesNotContain("AssignedCurrentAttemptReadService")
                .doesNotContain("SelfCurrentAttemptReadService")
                .doesNotContain("SelfVisibleTestingReadService")
                .doesNotContain("SelfHistoricalResultQueryService")
                .doesNotContain("findAssignment")
                .doesNotContain("findCourse")
                .doesNotContain("findTopic")
                .doesNotContain("findTest")
                .doesNotContain("findResult");
        }
    }

    @Test
    void notificationReadDtosMustRemainDeliveryStateModelsOnly() throws Exception {
        String adminResponse = read(NOTIFICATION_ADMIN_RESPONSE);
        String selfResponse = read(NOTIFICATION_SELF_RESPONSE);

        assertThat(adminResponse)
            .doesNotContain("Assignment")
            .doesNotContain("Course")
            .doesNotContain("Topic")
            .doesNotContain("Test")
            .doesNotContain("Result")
            .doesNotContain("Content")
            .doesNotContain("targetDetails")
            .doesNotContain("resolvedTarget")
            .doesNotContain("attemptDetails")
            .doesNotContain("grantedAccess");

        assertThat(selfResponse)
            .contains("String notificationType")
            .contains("String companyName")
            .contains("NotificationAssignmentRecipientResponse")
            .contains("String courseName")
            .doesNotContain("targetDetails")
            .doesNotContain("resolvedTarget")
            .doesNotContain("attemptDetails")
            .doesNotContain("grantedAccess");
    }

    @Test
    void notificationReferenceFieldsMustNotBecomePermissionGrant() throws Exception {
        String adminController = read(NOTIFICATION_ADMIN_CONTROLLER);
        String selfController = read(NOTIFICATION_SELF_CONTROLLER);
        String adminReadService = read(NOTIFICATION_ADMIN_READ_SERVICE);
        String selfReadService = read(NOTIFICATION_SELF_READ_SERVICE);

        assertThat(adminController)
            .contains("sourceEntityType")
            .contains("sourceEntityId")
            .doesNotContain("openTarget")
            .doesNotContain("resolveTarget")
            .doesNotContain("targetAccess")
            .doesNotContain("grantAccess");

        assertThat(selfController)
            .doesNotContain("openTarget")
            .doesNotContain("resolveTarget")
            .doesNotContain("targetAccess")
            .doesNotContain("grantAccess");

        assertThat(adminReadService)
            .doesNotContain("target-domain")
            .doesNotContain("permission grant")
            .doesNotContain("grantAccess")
            .doesNotContain("resolveTarget");

        assertThat(selfReadService)
            .doesNotContain("target-domain")
            .doesNotContain("permission grant")
            .doesNotContain("grantAccess")
            .doesNotContain("resolveTarget");
    }

    @Test
    void notificationReadMustStayOnDedicatedPolicyAwareNotificationContour() throws Exception {
        String accessPolicy = read(ACCESS_SPECIFICATION_POLICY);
        String resolver = read(QUERY_CONTEXT_RESOLVER);
        String selfReadService = read(NOTIFICATION_SELF_READ_SERVICE);
        String adminReadService = read(NOTIFICATION_ADMIN_READ_SERVICE);

        assertThat(accessPolicy)
            .contains("NOTIFICATION_ADMINISTRATION_TARGET_FAMILIES")
            .contains("NOTIFICATION_RECIPIENT_SELF_TARGET_FAMILIES")
            .doesNotContain("sourceEntityType grants")
            .doesNotContain("sourceEntityId grants");

        assertThat(resolver)
            .contains("resolveNotificationAdministrationContext")
            .contains("resolveNotificationAdministrationDetailContext")
            .contains("resolveNotificationRecipientSelfContext")
            .contains("resolveNotificationRecipientSelfDetailContext");

        assertThat(adminReadService)
            .contains("NOTIFICATION_ADMINISTRATION")
            .contains("AccessSpecificationPolicy")
            .contains("queryContextResolver.resolveNotificationAdministrationContext");

        assertThat(selfReadService)
            .contains("NOTIFICATION_RECIPIENT_SELF")
            .contains("AccessSpecificationPolicy")
            .contains("queryContextResolver.resolveNotificationRecipientSelfContext")
            .contains("findNotificationByIdAndRecipientUserId")
            .doesNotContain("findNotificationById(notificationId)");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
