package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение сервиса {@code NotificationRule}.
 * Сценарии сосредоточены на прикладной логике.
 */
class NotificationRuleServiceTest {

    @Test
    void notificationRuleServiceMustExposeDedicatedRuleCommandOperations() throws Exception {
        Set<String> interfaceMethods = Arrays.stream(NotificationRuleService.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(interfaceMethods)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleService must keep create/update and add dedicated enable/disable "
                    + "rule command operations, but current methods are %s",
                interfaceMethods
            )
            .contains("createNotificationRule", "updateNotificationRule", "enableNotificationRule", "disableNotificationRule");

        Class<?> implementationClass = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationRuleServiceImpl"
        );
        Set<String> implementationMethods = Arrays.stream(implementationClass.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());

        assertThat(implementationMethods)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl must implement dedicated enable/disable rule operations, "
                    + "but current methods are %s",
                implementationMethods
            )
            .contains("createNotificationRule", "updateNotificationRule", "enableNotificationRule", "disableNotificationRule");
    }

    @Test
    void notificationRuleServiceImplementationMustUseCommandAdmissionAndPreventSilentSemanticDuplicates() throws Exception {
        Class<?> implementationClass = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationRuleServiceImpl"
        );

        assertThat(hasConstructorDependency(implementationClass, NotificationRuleRepository.class)).isTrue();
        assertThat(hasConstructorDependency(implementationClass, CapabilityAdmissionPolicy.class))
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl must depend on CapabilityAdmissionPolicy "
                    + "to perform command admission before rule mutation."
            )
            .isTrue();
        assertThat(hasConstructorDependency(implementationClass, CapabilityAdmissionRequestFactory.class))
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl must depend on CapabilityAdmissionRequestFactory "
                    + "to build typed rule-command admission requests."
            )
            .isTrue();

        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        assertThat(source).contains("CapabilityAdmissionRequestFactory");
        assertThat(source).contains("CapabilityAdmissionPolicy");
        assertThat(source).doesNotContain("JpaAccessSpecificationPolicy");
        assertThat(source).doesNotContain("AccessPolicyQueryContextResolver");
        assertThat(source).doesNotContain("AccessReadArea");

        String createBody = extractMethodBody(source, "createNotificationRule");
        String updateBody = extractMethodBody(source, "updateNotificationRule");
        String enableBody = extractMethodBody(source, "enableNotificationRule");
        String disableBody = extractMethodBody(source, "disableNotificationRule");

        assertAdmissionBeforeMutation(
            createBody,
            "createNotificationRule",
            "createNotificationRuleCreate"
        );
        assertAdmissionBeforeMutation(
            updateBody,
            "updateNotificationRule",
            "createNotificationRuleUpdate"
        );
        assertAdmissionBeforeMutation(
            enableBody,
            "enableNotificationRule",
            "createNotificationRuleEnable"
        );
        assertAdmissionBeforeMutation(
            disableBody,
            "disableNotificationRule",
            "createNotificationRuleDisable"
        );

        assertSemanticDuplicateCheck(createBody, "createNotificationRule");
        assertSemanticDuplicateCheck(updateBody, "updateNotificationRule");
    }

    @Test
    void notificationRuleServiceMustRemainFutureOnlyAndOwnerDecoupled() throws Exception {
        assertControllerBoundaryRemainsReadOnlyAndRuleCommandFree();

        Path serviceRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/service");
        try (var files = Files.walk(serviceRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                assertBoundarySafe(Files.readString(javaFile));
            }
        }

        String ruleSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));
        assertThat(ruleSource).doesNotContain("NotificationRepository");
        assertThat(ruleSource).doesNotContain("saveNotification(");
        assertThat(ruleSource).doesNotContain("deleteNotification(");
        assertThat(ruleSource).doesNotContain("backfill");
        assertThat(ruleSource).doesNotContain("historical");
    }

    private void assertControllerBoundaryRemainsReadOnlyAndRuleCommandFree() throws Exception {
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
                assertThat(source).doesNotContain("JpaNotificationRuleRepositoryAdapter");
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

    private void assertAdmissionBeforeMutation(String methodBody, String methodName, String expectedBuilder) {
        assertThat(methodBody)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) must exist as a dedicated rule command path.",
                methodName
            )
            .isNotNull();

        int builderIndex = methodBody.indexOf(expectedBuilder + "(");
        int checkIndex = methodBody.indexOf("capabilityAdmissionPolicy.check(");
        int saveIndex = methodBody.indexOf("saveNotificationRule(");

        assertThat(builderIndex)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) must build typed command admission request via %s(...).",
                methodName,
                expectedBuilder
            )
            .isGreaterThanOrEqualTo(0);
        assertThat(checkIndex)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) must call capabilityAdmissionPolicy.check(...) before mutation.",
                methodName
            )
            .isGreaterThanOrEqualTo(0);
        assertThat(saveIndex)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) must still mutate only NotificationRuleRepository facts.",
                methodName
            )
            .isGreaterThanOrEqualTo(0);
        assertThat(checkIndex)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) must perform command admission before repository save.",
                methodName
            )
            .isLessThan(saveIndex);
    }

    private void assertSemanticDuplicateCheck(String methodBody, String methodName) {
        assertThat(methodBody)
            .withFailMessage("Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) method body is missing.", methodName)
            .isNotNull();

        boolean hasSemanticDuplicateCheck = methodBody.contains("findNotificationRuleByCode(")
            || methodBody.contains("findEnabledNotificationRules(")
            || methodBody.contains("findNotificationRulesByTypeAndChannel(");

        assertThat(hasSemanticDuplicateCheck)
            .withFailMessage(
                "Stage 3.1 gap: NotificationRuleServiceImpl.%s(...) must prevent active semantic duplicates "
                    + "through repository lookup before save when DB uniqueness does not cover the semantic case.",
                methodName
            )
            .isTrue();
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
        assertThat(source).doesNotContain("NotificationScheduler");
        assertThat(source).doesNotContain("DeadlineReminderScheduler");
        assertThat(source).doesNotContain("EmailNotificationProvider");
        assertThat(source).doesNotContain("SmsNotificationProvider");
        assertThat(source).doesNotContain("TelegramNotificationProvider");
        assertThat(source).doesNotContain("NotificationProviderRegistry");
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
