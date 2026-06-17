package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code PublicApiPerimeter} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class PublicApiPerimeterRegressionTest {

    private static final Path NOTIFICATION_CONTROLLER_ROOT = Path.of(
        "src/main/java/com/vladislav/training/platform/notification/controller"
    );
    private static final Path INTEGRATION_CONTROLLER_ROOT = Path.of(
        "src/main/java/com/vladislav/training/platform/integration/controller"
    );
    private static final Path AUDIT_CONTROLLER_ROOT = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/controller"
    );
    private static final Path AUDIT_ADMIN_READ_SERVICE = Path.of(
        "src/main/java/com/vladislav/training/platform/audit/service/AuditAdminReadServiceImpl.java"
    );
    private static final Path NOTIFICATION_ADMIN_READ_CONTROLLER = NOTIFICATION_CONTROLLER_ROOT.resolve(
        "NotificationAdminReadController.java"
    );
    private static final Path NOTIFICATION_SELF_READ_CONTROLLER = NOTIFICATION_CONTROLLER_ROOT.resolve(
        "NotificationSelfReadController.java"
    );
    private static final Path NOTIFICATION_ADMIN_COMMAND_CONTROLLER = NOTIFICATION_CONTROLLER_ROOT.resolve(
        "NotificationAdminCommandController.java"
    );
    private static final Path NOTIFICATION_SELF_COMMAND_CONTROLLER = NOTIFICATION_CONTROLLER_ROOT.resolve(
        "NotificationSelfCommandController.java"
    );
    private static final Path IMPORT_ADMIN_COMMAND_CONTROLLER = INTEGRATION_CONTROLLER_ROOT.resolve(
        "ImportAdminCommandController.java"
    );
    private static final Path IMPORT_ADMIN_READ_CONTROLLER = INTEGRATION_CONTROLLER_ROOT.resolve(
        "ImportAdminReadController.java"
    );
    private static final Path IMPORT_ITEM_REVIEW_CONTROLLER = INTEGRATION_CONTROLLER_ROOT.resolve(
        "ImportItemReviewController.java"
    );
    private static final Path AUDIT_ADMIN_READ_CONTROLLER = AUDIT_CONTROLLER_ROOT.resolve(
        "AuditAdminReadController.java"
    );

    @Test
    void administrativePublicControllersMustNotExposeGenericAdminOperationalOrTablesSurface() throws Exception {
        for (Path controller : administrativeControllerSources()) {
            String source = read(controller);
            assertThat(source)
                
                .doesNotContain("/api/v1/admin/operational")
                .doesNotContain("/api/v1/admin/tables")
                .doesNotContain("@RequestMapping(\"/operational")
                .doesNotContain("@RequestMapping(\"/tables")
                .doesNotContain("@GetMapping(\"/operational")
                .doesNotContain("@GetMapping(\"/tables")
                .doesNotContain("@PostMapping(\"/operational")
                .doesNotContain("@PostMapping(\"/tables");
        }
    }

    @Test
    void auditScn26MustRemainReadOnlyAndMustNotWireAuditWriteSide() throws Exception {
        String auditController = read(AUDIT_ADMIN_READ_CONTROLLER);
        String auditReadService = read(AUDIT_ADMIN_READ_SERVICE);

        assertThat(auditController)
            .contains("@RequestMapping(\"/api/v1/admin/audit-events\")")
            .contains("@GetMapping")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("/repair")
            .doesNotContain("/rebuild")
            .doesNotContain("/backfill")
            .doesNotContain("/replay")
            .doesNotContain("/recover")
            .doesNotContain("/reconcile")
            .doesNotContain("import com.vladislav.training.platform.audit.service.AuditService")
            .doesNotContain("recordAudit(")
            .doesNotContain("recordAuditEvent(");

        assertThat(auditReadService)
            .doesNotContain("import com.vladislav.training.platform.audit.service.AuditService")
            .doesNotContain("recordAudit(")
            .doesNotContain("recordAuditEvent(")
            .doesNotContain("auditService.");
    }

    @Test
    void importControllersMustExposeOnlyAcceptedTypedLaunchReadAndReviewRoutes() throws Exception {
        String commandController = read(IMPORT_ADMIN_COMMAND_CONTROLLER);
        String readController = read(IMPORT_ADMIN_READ_CONTROLLER);
        String reviewController = read(IMPORT_ITEM_REVIEW_CONTROLLER);

        assertThat(commandController)
            .contains("@RequestMapping(\"/api/v1/admin/import-jobs\")")
            .contains("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("patch-owner")
            .doesNotContain("owner-patch")
            .doesNotContain("target-patch")
            .doesNotContain("apply-owner")
            .doesNotContain("direct-update")
            .doesNotContain("direct-patch");

        assertThat(readController)
            .contains("@GetMapping(\"/import-jobs\")")
            .contains("@GetMapping(\"/import-jobs/{importJobId}\")")
            .contains("@GetMapping(\"/import-jobs/{importJobId}/items\")")
            .contains("@GetMapping(\"/import-job-items/{itemId}\")")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("patch-owner")
            .doesNotContain("owner-patch")
            .doesNotContain("target-patch")
            .doesNotContain("apply-owner")
            .doesNotContain("direct-update")
            .doesNotContain("direct-patch");

        assertThat(reviewController)
            .contains("@RequestMapping(\"/api/v1/admin/import-job-items\")")
            .contains("@PostMapping(\"/{itemId}/apply-review\")")
            .contains("@PostMapping(\"/{itemId}/reject-review\")")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping")
            .doesNotContain("patch-owner")
            .doesNotContain("owner-patch")
            .doesNotContain("target-patch")
            .doesNotContain("apply-owner")
            .doesNotContain("direct-update")
            .doesNotContain("direct-patch");
    }

    @Test
    void notificationControllersMustNotExposeTargetAccessRoutesOrGenericCrud() throws Exception {
        String adminController = read(NOTIFICATION_ADMIN_READ_CONTROLLER);
        String selfController = read(NOTIFICATION_SELF_READ_CONTROLLER);
        String adminCommandController = read(NOTIFICATION_ADMIN_COMMAND_CONTROLLER);
        String selfCommandController = read(NOTIFICATION_SELF_COMMAND_CONTROLLER);

        assertThat(adminController)
            .contains("@RequestMapping(\"/api/v1/admin/notifications\")")
            .contains("@GetMapping")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");
        assertThat(selfController)
            .contains("@RequestMapping(\"/api/v1/self/notifications\")")
            .contains("@GetMapping")
            .doesNotContain("@PostMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");
        assertThat(adminCommandController)
            .contains("@RequestMapping(\"/api/v1/admin/notifications\")")
            .contains("@PostMapping(\"/dispatch-pending\")")
            .doesNotContain("@GetMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");
        assertThat(selfCommandController)
            .contains("@RequestMapping(\"/api/v1/self/notifications\")")
            .contains("@PostMapping(\"/{notificationId}/read\")")
            .contains("@PostMapping(\"/read-all\")")
            .doesNotContain("@GetMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@PutMapping")
            .doesNotContain("@DeleteMapping");

        assertNoNotificationTargetRouteDrift(adminController);
        assertNoNotificationTargetRouteDrift(selfController);
        assertNoNotificationTargetRouteDrift(adminCommandController);
        assertNoNotificationTargetRouteDrift(selfCommandController);
    }

    @Test
    void administrativePublicControllerPostRoutesMustRemainTypedCommandsOnly() throws Exception {
        assertThat(read(IMPORT_ADMIN_COMMAND_CONTROLLER))
            .contains("@PostMapping");
        assertThat(read(IMPORT_ITEM_REVIEW_CONTROLLER))
            .contains("@PostMapping(\"/{itemId}/apply-review\")")
            .contains("@PostMapping(\"/{itemId}/reject-review\")");

        for (Path controller : administrativeControllerSources()) {
            String source = read(controller);
            String fileName = controller.getFileName().toString();
            if (fileName.equals("ImportAdminCommandController.java")
                || fileName.equals("ImportItemReviewController.java")
                || fileName.equals("NotificationAdminCommandController.java")
                || fileName.equals("NotificationSelfCommandController.java")) {
                continue;
            }

            assertThat(source)
                
                .doesNotContain("@PostMapping");
        }
    }

    private void assertNoNotificationTargetRouteDrift(String source) {
        assertThat(source)
            .doesNotContain("@RequestMapping(\"/target")
            .doesNotContain("@GetMapping(\"/target")
            .doesNotContain("@GetMapping(\"/{notificationId}/target")
            .doesNotContain("@GetMapping(\"/assignment")
            .doesNotContain("@GetMapping(\"/content")
            .doesNotContain("@GetMapping(\"/testing")
            .doesNotContain("@GetMapping(\"/attempt")
            .doesNotContain("@GetMapping(\"/result")
            .doesNotContain("open-target")
            .doesNotContain("resolve-target");
    }

    private List<Path> administrativeControllerSources() throws IOException {
        return Stream.of(
                controllerSources(NOTIFICATION_CONTROLLER_ROOT),
                controllerSources(INTEGRATION_CONTROLLER_ROOT),
                controllerSources(AUDIT_CONTROLLER_ROOT)
            )
            .flatMap(List::stream)
            .toList();
    }

    private List<Path> controllerSources(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(path -> path.toString().endsWith("Controller.java"))
                .toList();
        }
    }

    private String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
