package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение сервиса {@code NotificationDispatch}.
 * Сценарии сосредоточены на прикладной логике.
 */
class NotificationDispatchServiceTest {

    @Test
    void dispatchServiceMustExposeCanonicalPendingDispatchOperation() {
        Set<String> methodSignatures = Arrays.stream(NotificationDispatchService.class.getDeclaredMethods())
            .map(this::signature)
            .collect(Collectors.toUnmodifiableSet());

        boolean hasEquivalentPendingDispatchOperation = methodSignatures.contains(
            "dispatchPendingNotifications(java.time.Instant,int)"
        ) || methodSignatures.contains("dispatchEligibleNotifications(java.time.Instant,int)")
            || methodSignatures.contains("processPendingNotifications(java.time.Instant,int)")
            || methodSignatures.contains("dispatchDueNotifications(java.time.Instant,int)");

        assertThat(hasEquivalentPendingDispatchOperation)
            .withFailMessage(
                "Canonical Step 2.4 gap: NotificationDispatchService has no dispatch-pending operation. "
                    + "Expected dedicated method like dispatchPendingNotifications(Instant now, int limit), "
                    + "but found only: %s",
                methodSignatures
            )
            .isTrue();
    }

    @Test
    void notificationRepositoryContourMustExposePendingEligibleDispatchCandidateLookup() {
        Set<String> repositoryMethods = Arrays.stream(
            com.vladislav.training.platform.notification.repository.NotificationRepository.class.getDeclaredMethods()
        ).map(this::signature).collect(Collectors.toUnmodifiableSet());

        Set<String> springDataMethods = Arrays.stream(
            com.vladislav.training.platform.notification.infrastructure.persistence.SpringDataNotificationJpaRepository.class
                .getDeclaredMethods()
        ).map(this::signature).collect(Collectors.toUnmodifiableSet());

        boolean hasRepositoryEquivalent = repositoryMethods.contains(
            "findPendingEligibleDispatchNotifications(java.time.Instant,int)"
        ) || repositoryMethods.contains("findDispatchCandidates(java.time.Instant,int)")
            || repositoryMethods.contains("findPendingDispatchCandidates(java.time.Instant,int)")
            || repositoryMethods.contains("findEligibleNotificationsForDispatch(java.time.Instant,int)");

        boolean hasSpringDataEquivalent = springDataMethods.contains(
            "findPendingEligibleDispatchNotifications(java.time.Instant,int)"
        ) || springDataMethods.contains("findDispatchCandidates(java.time.Instant,int)")
            || springDataMethods.contains("findPendingDispatchCandidates(java.time.Instant,int)")
            || springDataMethods.contains("findEligibleNotificationsForDispatch(java.time.Instant,int)");

        assertThat(hasRepositoryEquivalent && hasSpringDataEquivalent)
            .withFailMessage(
                "Canonical Step 2.4 gap: notification repository contour has no dedicated pending-eligible dispatch "
                    + "candidate lookup. Repository methods: %s ; Spring Data methods: %s",
                repositoryMethods,
                springDataMethods
            )
            .isTrue();
    }

    @Test
    void dispatchImplementationMustHaveProviderSuccessFailureSeamBeforeSentTransitionPath() throws Exception {
        List<String> supportedSeamNames = List.of(
            "com.vladislav.training.platform.notification.service.NotificationDeliveryProvider",
            "com.vladislav.training.platform.notification.service.NotificationChannelProvider",
            "com.vladislav.training.platform.notification.service.NotificationDeliveryGateway"
        );

        boolean hasDeliverySeam = supportedSeamNames.stream().anyMatch(this::classExists);

        assertThat(hasDeliverySeam)
            .withFailMessage(
                "Canonical Step 2.4 gap: no provider/gateway seam found for Notification dispatch. "
                    + "Expected one of %s before SENT can be tied to provider success.",
                supportedSeamNames
            )
            .isTrue();

        String dispatchSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationDispatchServiceImpl.java"
        ));
        assertBoundarySafe(dispatchSource);
    }

    @Test
    void notificationPackagesMustStayOwnerDecoupledAndControllerFree() throws Exception {
        assertPackageSafe(Path.of("src/main/java/com/vladislav/training/platform/notification/service"));
        assertPackageSafe(Path.of("src/main/java/com/vladislav/training/platform/notification/infrastructure/persistence"));
        assertControllerPackageStaysTypedReadOnlyBoundary();

        String commandSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"
        ));
        String querySource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"
        ));
        String ruleSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));

        assertThat(commandSource).doesNotContain("NotificationDispatchService");
        assertThat(querySource).doesNotContain("NotificationDispatchService");
        assertThat(ruleSource).doesNotContain("NotificationDispatchService");
        assertThat(querySource).doesNotContain(".save(");
        assertThat(querySource).doesNotContain(".update(");
        assertThat(ruleSource).doesNotContain("NotificationRepository");
    }

    private void assertControllerPackageStaysTypedReadOnlyBoundary() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);
                assertThat(source).doesNotContain("AssignmentEntity");
                assertThat(source).doesNotContain("TestAttemptEntity");
                assertThat(source).doesNotContain("ResultEntity");
                assertThat(source).doesNotContain("CourseEntity");
                assertThat(source).doesNotContain("UserEntity");
                assertThat(source).doesNotContain("AssignmentRepository");
                assertThat(source).doesNotContain("TestAttemptRepository");
                assertThat(source).doesNotContain("ResultRepository");
                assertThat(source).doesNotContain("CourseRepository");
                assertThat(source).doesNotContain("SpringDataNotificationJpaRepository");
                assertThat(source).doesNotContain("SpringDataNotificationRuleJpaRepository");
                assertThat(source).doesNotContain("JpaNotificationRepositoryAdapter");
                assertThat(source).doesNotContain("JpaNotificationRuleRepositoryAdapter");
                assertThat(source).doesNotContain("NotificationDispatchService");
                assertThat(source).doesNotContain("NotificationRuleService");
                assertThat(source).doesNotContain(".save(");
                assertThat(source).doesNotContain(".update(");
                assertThat(source).doesNotContain(".delete(");
            }
        }
    }

    private void assertPackageSafe(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                assertBoundarySafe(Files.readString(javaFile));
            }
        }
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

        assertThat(source).doesNotContain("@RestController");
        assertThat(source).doesNotContain("@Controller");
        assertThat(source).doesNotContain("@RequestMapping");
        assertThat(source).doesNotContain("@GetMapping");
        assertThat(source).doesNotContain("@PostMapping");
        assertThat(source).doesNotContain("@PutMapping");
        assertThat(source).doesNotContain("@PatchMapping");
        assertThat(source).doesNotContain("@DeleteMapping");
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private String signature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
            .map(Class::getName)
            .collect(Collectors.joining(","));
        return method.getName() + "(" + parameters + ")";
    }
}
