package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * Проверяет вспомогательную логику {@code NotificationRuleCriticalAudit}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
class NotificationRuleCriticalAuditSupportTest {

    @Test
    void successfulNotificationRuleCommandsClassifiedAsCriticalMustHaveSynchronousAuditCompanion() throws Exception {
        Class<?> implementationClass = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationRuleServiceImpl"
        );
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        assertThat(hasConstructorDependency(implementationClass, CriticalCommandAuditSupport.class))
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl must depend on CriticalCommandAuditSupport when "
                    + "notification rule commands are classified as critical operational configuration commands."
            )
            .isTrue();
        assertThat(source)
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl must keep synchronous critical audit companion seams "
                    + "and currently does not reference CriticalCommandAuditSupport."
            )
            .contains("CriticalCommandAuditSupport")
            .contains("recordAudit(")
            .contains("buildAuditContext(")
            .contains("AuditEventType");
        assertThat(source)
            .doesNotContain("@Async")
            .doesNotContain("ApplicationEventPublisher")
            .doesNotContain("publishEvent(")
            .doesNotContain("CompletableFuture");
    }

    @Test
    void ruleCreateUpdateEnableDisableMustWriteSuccessAuditAndDeniedCommandMustNotEmitSuccessAudit() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        String createBody = extractMethodBody(source, "createNotificationRule");
        String updateBody = extractMethodBody(source, "updateNotificationRule");
        String enableBody = extractMethodBody(source, "enableNotificationRule");
        String disableBody = extractMethodBody(source, "disableNotificationRule");

        assertSynchronousAuditAfterMutation(createBody, "createNotificationRule");
        assertSynchronousAuditAfterMutation(updateBody, "updateNotificationRule");
        assertSynchronousAuditAfterMutation(enableBody, "enableNotificationRule");
        assertSynchronousAuditAfterMutation(disableBody, "disableNotificationRule");

        assertThat(source)
            .withFailMessage(
                "Stage 3.2 gap: Notification rule success audit payload must reference rule id, rule code and "
                    + "action=create/update/enable/disable for synchronous companion writes."
            )
            .contains("\"ruleId\"")
            .contains("\"ruleCode\"")
            .contains("\"action\"")
            .contains("\"create\"")
            .contains("\"update\"")
            .contains("\"enable\"")
            .contains("\"disable\"");
    }

    @Test
    void criticalAuditCompanionBoundaryMustRemainFutureOnlyAndOwnerDecoupled() throws Exception {
        assertControllerBoundaryRemainsReadOnlyAndAuditCompanionFree();

        List<Path> ruleServiceFiles = List.of(
            Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleService.java"),
            Path.of("src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java")
        );
        for (Path javaFile : ruleServiceFiles) {
            assertBoundarySafe(Files.readString(javaFile));
        }

        String ruleSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));
        assertThat(ruleSource).doesNotContain("NotificationRepository");
        assertThat(ruleSource).doesNotContain("deleteNotification(");
        assertThat(ruleSource).doesNotContain("saveNotification(");
        assertThat(ruleSource).doesNotContain("rewrite historical");
        assertThat(ruleSource).doesNotContain("historical notification");
        assertThat(ruleSource).doesNotContain("backfill");
        assertThat(ruleSource).doesNotContain("old windows");
    }

    private void assertControllerBoundaryRemainsReadOnlyAndAuditCompanionFree() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists().isDirectory();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);
                String fileName = javaFile.getFileName().toString();
                boolean lifecycleCommandController = fileName.equals("NotificationAdminCommandController.java")
                    || fileName.equals("NotificationSelfCommandController.java");
                assertThat(source).doesNotContain("NotificationRuleService");
                assertThat(source).doesNotContain("CriticalCommandAuditSupport");
                assertThat(source).doesNotContain("NotificationRuleRepository");
                assertThat(source).doesNotContain("recordAudit(");
                assertThat(source).doesNotContain("saveNotificationRule(");
                assertThat(source).doesNotContain("deleteNotificationRule(");
                if (!lifecycleCommandController) {
                    assertThat(source).doesNotContain("@PostMapping");
                }
                assertThat(source).doesNotContain("@PutMapping");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
                assertThat(source).doesNotContain("AssignmentRepository");
                assertThat(source).doesNotContain("ResultRepository");
                assertThat(source).doesNotContain("CourseRepository");
                assertThat(source).doesNotContain("TestAttemptRepository");
            }
        }
    }

    private void assertSynchronousAuditAfterMutation(String methodBody, String methodName) {
        assertThat(methodBody)
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl.%s(...) must exist to attach synchronous critical audit.",
                methodName
            )
            .isNotNull();

        int admissionIndex = methodBody.indexOf("capabilityAdmissionPolicy.check(");
        int saveIndex = methodBody.indexOf("saveNotificationRule(");
        int auditIndex = methodBody.indexOf("recordAudit(");

        assertThat(admissionIndex)
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl.%s(...) must keep command admission before mutation.",
                methodName
            )
            .isGreaterThanOrEqualTo(0);
        assertThat(saveIndex)
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl.%s(...) must persist notification rule fact before success audit.",
                methodName
            )
            .isGreaterThanOrEqualTo(0);
        assertThat(auditIndex)
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl.%s(...) must write synchronous success audit "
                    + "through CriticalCommandAuditSupport after successful rule mutation.",
                methodName
            )
            .isGreaterThanOrEqualTo(0);
        assertThat(auditIndex)
            .withFailMessage(
                "Stage 3.2 gap: NotificationRuleServiceImpl.%s(...) must not emit success audit before repository mutation succeeds.",
                methodName
            )
            .isGreaterThan(saveIndex);
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
        assertThat(source).doesNotContain("recordResult");
        assertThat(source).doesNotContain("submitAttempt");
        assertThat(source).doesNotContain("closeAssignment");
        assertThat(source).doesNotContain("patchOwner");
        assertThat(source).doesNotContain("ownerTable");
        assertThat(source).doesNotContain("databaseTable");
        assertThat(source).doesNotContain("EmailNotificationProvider");
        assertThat(source).doesNotContain("SmsNotificationProvider");
        assertThat(source).doesNotContain("TelegramNotificationProvider");
        assertThat(source).doesNotContain("NotificationProviderRegistry");
        assertThat(source).doesNotContain("NotificationScheduler");
        assertThat(source).doesNotContain("DeadlineReminderScheduler");
        assertThat(source).doesNotContain("NotificationDispatchService");
        assertThat(source).doesNotContain("NotificationDeliveryGateway");
        assertThat(source).doesNotContain("NotificationDeliveryResult");
        assertThat(source).doesNotContain("@RestController");
        assertThat(source).doesNotContain("@Controller");
        assertThat(source).doesNotContain("@RequestMapping");
        assertThat(source).doesNotContain("@GetMapping");
        assertThat(source).doesNotContain("@PostMapping");
        assertThat(source).doesNotContain("@PutMapping");
        assertThat(source).doesNotContain("@PatchMapping");
        assertThat(source).doesNotContain("@DeleteMapping");
    }

    private boolean hasConstructorDependency(Class<?> type, Class<?> dependencyType) {
        return Arrays.stream(type.getDeclaredConstructors())
            .map(Constructor::getParameterTypes)
            .flatMap(Arrays::stream)
            .anyMatch(parameterType -> parameterType.equals(dependencyType));
    }

    private String extractMethodBody(String source, String methodName) {
        int signatureStart = source.indexOf(methodName + "(");
        if (signatureStart < 0) {
            return null;
        }

        int bodyStart = source.indexOf('{', signatureStart);
        if (bodyStart < 0) {
            return null;
        }

        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart + 1, index);
                }
            }
        }

        return null;
    }
}
