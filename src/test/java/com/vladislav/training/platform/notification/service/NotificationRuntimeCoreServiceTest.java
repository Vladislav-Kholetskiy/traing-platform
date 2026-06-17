package com.vladislav.training.platform.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.notification.domain.Notification;
import com.vladislav.training.platform.notification.domain.NotificationChannel;
import com.vladislav.training.platform.notification.domain.NotificationRule;
import com.vladislav.training.platform.notification.domain.NotificationStatus;
import com.vladislav.training.platform.notification.repository.NotificationRepository;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение сервиса {@code NotificationRuntimeCore}.
 * Сценарии сосредоточены на прикладной логике.
 */
class NotificationRuntimeCoreServiceTest {

    @Test
    void notificationRuntimeCoreInterfacesStayLockedToStageTwoContracts() {
        assertInterfaceContract(
            NotificationCommandService.class,
            Set.of(
                signature("createNotification", Notification.class, Notification.class),
                signature("scheduleNotification", Notification.class, Long.class, Instant.class),
                signature("markNotificationRead", Notification.class, Long.class, Long.class, Instant.class),
                signature("markAllNotificationsRead", int.class, Long.class, Instant.class)
            )
        );
        assertInterfaceContract(
            NotificationRuleService.class,
            Set.of(
                signature("createNotificationRule", NotificationRule.class, NotificationRule.class),
                signature("updateNotificationRule", NotificationRule.class, NotificationRule.class),
                signature("enableNotificationRule", NotificationRule.class, Long.class),
                signature("disableNotificationRule", NotificationRule.class, Long.class),
                signature("findNotificationRuleById", NotificationRule.class, Long.class),
                signature("findNotificationRuleByCode", NotificationRule.class, String.class),
                signature("findEnabledNotificationRules", List.class),
                signature("findNotificationRulesByTypeAndChannel", List.class, String.class, NotificationChannel.class)
            )
        );
        assertInterfaceContract(
            NotificationQueryService.class,
            Set.of(
                signature("findNotificationById", Notification.class, Long.class),
                signature("findNotificationByIdAndRecipientUserId", Notification.class, Long.class, Long.class),
                signature("findNotificationsByRecipientUserId", List.class, Long.class),
                signature("findNotificationsByStatus", List.class, NotificationStatus.class),
                signature("findNotificationsScheduledAtOrBefore", List.class, Instant.class),
                signature("findNotificationsBySourceEntity", List.class, String.class, String.class),
                signature("findNotificationsByDedupKey", List.class, String.class)
            )
        );
        assertInterfaceContract(
            NotificationDispatchService.class,
            Set.of(
                signature("dispatchPendingNotifications", int.class, Instant.class, int.class),
                signature("registerDispatchAttempt", Notification.class, Long.class),
                signature("markNotificationSent", Notification.class, Long.class, Instant.class),
                signature("markNotificationFailed", Notification.class, Long.class, String.class, String.class)
            )
        );
    }

    @Test
    void stageTwoRuntimeCoreImplementationClassesMustExistAndStayRepositoryBounded() throws Exception {
        Class<?> commandImpl = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationCommandServiceImpl"
        );
        Class<?> ruleImpl = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationRuleServiceImpl"
        );
        Class<?> queryImpl = Class.forName(
            "com.vladislav.training.platform.notification.service.NotificationQueryServiceImpl"
        );

        assertThat(NotificationCommandService.class).isAssignableFrom(commandImpl);
        assertThat(NotificationRuleService.class).isAssignableFrom(ruleImpl);
        assertThat(NotificationQueryService.class).isAssignableFrom(queryImpl);

        assertThat(hasConstructorDependency(commandImpl, NotificationRepository.class)).isTrue();
        assertThat(hasConstructorDependency(ruleImpl, NotificationRuleRepository.class)).isTrue();
        assertThat(hasConstructorDependency(queryImpl, NotificationRepository.class)).isTrue();

        String commandSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationCommandServiceImpl.java"
        ));
        String ruleSource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationRuleServiceImpl.java"
        ));
        String querySource = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationQueryServiceImpl.java"
        ));

        assertRepositoryBounded(commandSource);
        assertRepositoryBounded(ruleSource);
        assertRepositoryBounded(querySource);

        assertThat(commandSource).doesNotContain("NotificationDispatchService");
        assertThat(querySource).doesNotContain(".save(");
        assertThat(querySource).doesNotContain("setStatus(");
        assertThat(ruleSource).doesNotContain("NotificationRepository");
    }

    @Test
    void notificationServiceBoundaryStaysRepositoryFreeAndDoesNotPullExternalProvidersTooEarly() throws Exception {
        assertControllerBoundaryRemainsRepositoryAndProviderFree();

        Path serviceRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/service");
        try (var files = Files.walk(serviceRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("AssignmentEntity");
                assertThat(source).doesNotContain("TestAttemptEntity");
                assertThat(source).doesNotContain("ResultEntity");
                assertThat(source).doesNotContain("CourseEntity");
                assertThat(source).doesNotContain("UserEntity");
                assertThat(source).doesNotContain("SpringDataAssignment");
                assertThat(source).doesNotContain("SpringDataTestAttempt");
                assertThat(source).doesNotContain("SpringDataResult");
                assertThat(source).doesNotContain("SpringDataCourse");
            }
        }

        assertThat(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationProvider.java"
        )).doesNotExist();
        assertThat(Path.of(
            "src/main/java/com/vladislav/training/platform/notification/service/NotificationScheduler.java"
        )).doesNotExist();
    }

    private void assertControllerBoundaryRemainsRepositoryAndProviderFree() throws Exception {
        Path controllerRoot = Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
        assertThat(controllerRoot).exists().isDirectory();

        try (var files = Files.walk(controllerRoot)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("NotificationRuleService");
                assertThat(source).doesNotContain("NotificationRepository");
                assertThat(source).doesNotContain("NotificationRuleRepository");
                assertThat(source).doesNotContain("NotificationProvider");
                assertThat(source).doesNotContain("@PatchMapping");
                assertThat(source).doesNotContain("@DeleteMapping");
            }
        }
    }

    private void assertRepositoryBounded(String source) {
        assertThat(source).doesNotContain("AssignmentEntity");
        assertThat(source).doesNotContain("TestAttemptEntity");
        assertThat(source).doesNotContain("ResultEntity");
        assertThat(source).doesNotContain("CourseEntity");
        assertThat(source).doesNotContain("UserEntity");
        assertThat(source).doesNotContain("AssignmentRepository");
        assertThat(source).doesNotContain("TestAttemptRepository");
        assertThat(source).doesNotContain("ResultRepository");
        assertThat(source).doesNotContain("CourseRepository");
        assertThat(source).doesNotContain("AuditService");
    }

    private boolean hasConstructorDependency(Class<?> type, Class<?> dependencyType) {
        return Arrays.stream(type.getDeclaredConstructors())
            .map(Constructor::getParameterTypes)
            .flatMap(Arrays::stream)
            .anyMatch(parameterType -> parameterType.equals(dependencyType));
    }

    private void assertInterfaceContract(Class<?> type, Set<MethodSignature> expectedSignatures) {
        assertThat(type.isInterface()).isTrue();
        assertThat(type.getDeclaredFields()).isEmpty();
        assertThat(Arrays.stream(type.getDeclaredMethods()).anyMatch(Method::isDefault)).isFalse();
        assertThat(Arrays.stream(type.getDeclaredMethods())
            .map(this::signatureOf)
            .collect(Collectors.toUnmodifiableSet()))
            .isEqualTo(expectedSignatures);
    }

    private MethodSignature signatureOf(Method method) {
        return new MethodSignature(method.getName(), method.getReturnType(), Arrays.asList(method.getParameterTypes()));
    }

    private MethodSignature signature(String name, Class<?> returnType, Class<?>... parameterTypes) {
        return new MethodSignature(name, returnType, Arrays.asList(parameterTypes));
    }

    private record MethodSignature(String name, Class<?> returnType, List<Class<?>> parameterTypes) {
    }
}
