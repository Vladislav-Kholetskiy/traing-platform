package com.vladislav.training.platform.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.vladislav.training.platform.notification.repository.NotificationRepository;
import com.vladislav.training.platform.notification.repository.NotificationRuleRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
/**
 * Проверяет форму и состав {@code NotificationPersistence}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class NotificationPersistenceShapeTest {

    private static final String PERSISTENCE_PACKAGE =
        "com.vladislav.training.platform.notification.infrastructure.persistence.";
    private static final Path V100_SCHEMA = Path.of("src/main/resources/db/migration/V100__full_schema_stack.sql");
    private static final Path NOTIFICATION_CONTROLLER_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/notification/controller");
    private static final Path NOTIFICATION_PERSISTENCE_PACKAGE =
        Path.of("src/main/java/com/vladislav/training/platform/notification/infrastructure/persistence");

    @Test
    void notificationPersistenceContourClassesMustExist() {
        assertThatCode(() -> requireClass("NotificationEntity")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("NotificationRuleEntity")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("SpringDataNotificationJpaRepository")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("SpringDataNotificationRuleJpaRepository")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("JpaNotificationRepositoryAdapter")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("JpaNotificationRuleRepositoryAdapter")).doesNotThrowAnyException();
        assertThatCode(() -> requireClass("NotificationMapper")).doesNotThrowAnyException();
    }

    @Test
    void notificationEntitiesMustMatchAdministrativeJpaAndJsonbShape() throws Exception {
        Class<?> notificationEntityType = requireClass("NotificationEntity");
        Class<?> notificationRuleEntityType = requireClass("NotificationRuleEntity");

        assertScalarEntityFieldSet(
            notificationEntityType,
            Set.of(
                "id",
                "recipientUserId",
                "notificationType",
                "channelCode",
                "status",
                "dedupKey",
                "sourceEntityType",
                "sourceEntityId",
                "scheduledAt",
                "sentAt",
                "readAt",
                "deliveryAttemptCount",
                "errorCode",
                "errorMessage",
                "payloadSnapshot",
                "createdAt",
                "updatedAt"
            )
        );
        assertTable(notificationEntityType, "notification");
        assertIdField(notificationEntityType, "id");
        assertColumn(notificationEntityType, "recipientUserId", "recipient_user_id", false);
        assertColumn(notificationEntityType, "notificationType", "notification_type", false);
        assertColumn(notificationEntityType, "channelCode", "channel_code", false);
        assertColumn(notificationEntityType, "status", "status", false);
        assertColumn(notificationEntityType, "dedupKey", "dedup_key", false);
        assertColumn(notificationEntityType, "sourceEntityType", "source_entity_type", true);
        assertColumn(notificationEntityType, "sourceEntityId", "source_entity_id", true);
        assertColumn(notificationEntityType, "scheduledAt", "scheduled_at", true);
        assertColumn(notificationEntityType, "sentAt", "sent_at", true);
        assertColumn(notificationEntityType, "readAt", "read_at", true);
        assertColumn(notificationEntityType, "deliveryAttemptCount", "delivery_attempt_count", false);
        assertColumn(notificationEntityType, "errorCode", "error_code", true);
        assertColumn(notificationEntityType, "errorMessage", "error_message", true);
        assertColumn(notificationEntityType, "payloadSnapshot", "payload_snapshot", true);
        assertColumn(notificationEntityType, "createdAt", "created_at", false);
        assertColumn(notificationEntityType, "updatedAt", "updated_at", false);
        assertJsonbField(notificationEntityType, "payloadSnapshot");
        assertPlainPersistenceCarrier(notificationEntityType);

        assertScalarEntityFieldSet(
            notificationRuleEntityType,
            Set.of(
                "id",
                "ruleCode",
                "name",
                "notificationType",
                "channelCode",
                "enabled",
                "daysBeforeDeadline",
                "repeatIntervalDays",
                "triggerMode",
                "recipientScopeCode",
                "createdAt",
                "updatedAt"
            )
        );
        assertTable(notificationRuleEntityType, "notification_rule");
        assertIdField(notificationRuleEntityType, "id");
        assertColumn(notificationRuleEntityType, "ruleCode", "rule_code", false);
        assertColumn(notificationRuleEntityType, "name", "name", false);
        assertColumn(notificationRuleEntityType, "notificationType", "notification_type", false);
        assertColumn(notificationRuleEntityType, "channelCode", "channel_code", false);
        assertColumn(notificationRuleEntityType, "enabled", "is_enabled", false);
        assertColumn(notificationRuleEntityType, "daysBeforeDeadline", "days_before_deadline", true);
        assertColumn(notificationRuleEntityType, "repeatIntervalDays", "repeat_interval_days", true);
        assertColumn(notificationRuleEntityType, "triggerMode", "trigger_mode", true);
        assertColumn(notificationRuleEntityType, "recipientScopeCode", "recipient_scope_code", true);
        assertColumn(notificationRuleEntityType, "createdAt", "created_at", false);
        assertColumn(notificationRuleEntityType, "updatedAt", "updated_at", false);
        assertPlainPersistenceCarrier(notificationRuleEntityType);
    }

    @Test
    void notificationRepositoriesAdaptersAndMapperMustStayScalarAndOwnerDecoupled() throws Exception {
        Class<?> springDataNotificationRepository = requireClass("SpringDataNotificationJpaRepository");
        Class<?> springDataNotificationRuleRepository = requireClass("SpringDataNotificationRuleJpaRepository");
        Class<?> notificationRepositoryAdapter = requireClass("JpaNotificationRepositoryAdapter");
        Class<?> notificationRuleRepositoryAdapter = requireClass("JpaNotificationRuleRepositoryAdapter");
        Class<?> notificationMapper = requireClass("NotificationMapper");

        assertThat(notificationRepositoryAdapter.getInterfaces()).contains(NotificationRepository.class);
        assertThat(notificationRuleRepositoryAdapter.getInterfaces()).contains(NotificationRuleRepository.class);
        assertThat(notificationMapper.isAnnotationPresent(Component.class)).isTrue();

        assertThat(springDataNotificationRepository.getSimpleName()).contains("SpringDataNotificationJpaRepository");
        assertThat(springDataNotificationRuleRepository.getSimpleName()).contains("SpringDataNotificationRuleJpaRepository");

        if (Files.exists(NOTIFICATION_PERSISTENCE_PACKAGE)) {
            String persistenceSource = Files.walk(NOTIFICATION_PERSISTENCE_PACKAGE)
                .filter(path -> path.toString().endsWith(".java"))
                .map(this::readString)
                .collect(Collectors.joining("\n"));

            assertThat(persistenceSource)
                .doesNotContain("AssignmentEntity")
                .doesNotContain("TestAttemptEntity")
                .doesNotContain("ResultEntity")
                .doesNotContain("CourseEntity")
                .doesNotContain("UserEntity");
        }
    }

    @Test
    void stageTwoPointOneMustStayControllerFreeAndUseExistingV100NotificationSchema() throws Exception {
        assertControllerBoundaryRemainsReadOnlyAndPersistenceDecoupled();

        String normalizedSchema = Files.readString(V100_SCHEMA).toLowerCase();
        assertThat(normalizedSchema)
            .contains("create table notification (")
            .contains("payload_snapshot jsonb")
            .contains("create table notification_rule (")
            .contains("dedup_key")
            .contains("deadline_reminder_7d");
        String readAtMigration = Files.readString(Path.of(
            "src/main/resources/db/migration/V109__add_notification_read_at.sql"
        )).toLowerCase();
        assertThat(readAtMigration)
            .contains("add column read_at")
            .contains("ix_notif__read_at");
    }

    private void assertControllerBoundaryRemainsReadOnlyAndPersistenceDecoupled() throws Exception {
        assertThat(Files.exists(NOTIFICATION_CONTROLLER_PACKAGE)).isTrue();

        String controllerSource = Files.walk(NOTIFICATION_CONTROLLER_PACKAGE)
            .filter(path -> path.toString().endsWith(".java"))
            .map(this::readString)
            .collect(Collectors.joining("\n"));

        assertThat(controllerSource)
            .doesNotContain("SpringDataNotificationJpaRepository")
            .doesNotContain("SpringDataNotificationRuleJpaRepository")
            .doesNotContain("JpaNotificationRepositoryAdapter")
            .doesNotContain("JpaNotificationRuleRepositoryAdapter")
            .doesNotContain("NotificationMapper")
            .doesNotContain("NotificationEntity")
            .doesNotContain("NotificationRuleEntity")
            .doesNotContain("saveNotification(")
            .doesNotContain("saveNotificationRule(")
            .doesNotContain("deleteNotification(")
            .doesNotContain("deleteNotificationRule(")
            .doesNotContain("@PutMapping")
            .doesNotContain("@PatchMapping")
            .doesNotContain("@DeleteMapping");
    }

    private Class<?> requireClass(String simpleName) throws ClassNotFoundException {
        return Class.forName(PERSISTENCE_PACKAGE + simpleName);
    }

    private void assertTable(Class<?> entityType, String expectedTableName) {
        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(entityType.isAnnotationPresent(Table.class)).isTrue();
        assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(expectedTableName);
    }

    private void assertIdField(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Id.class)).isTrue();
        assertThat(field.isAnnotationPresent(GeneratedValue.class)).isTrue();
    }

    private void assertScalarEntityFieldSet(Class<?> entityType, Set<String> expectedFieldNames) {
        assertThat(fieldNames(entityType)).isEqualTo(expectedFieldNames);
    }

    private void assertPlainPersistenceCarrier(Class<?> entityType) {
        boolean persistenceAnnotationsOnly = Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class)
                || field.isAnnotationPresent(JdbcTypeCode.class));
        boolean scalarFieldTypesOnly = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);
        boolean noCollectionFields = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .noneMatch(Collection.class::isAssignableFrom);

        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(persistenceAnnotationsOnly).isTrue();
        assertThat(scalarFieldTypesOnly).isTrue();
        assertThat(noCollectionFields).isTrue();
    }

    private void assertColumn(Class<?> entityType, String fieldName, String expectedColumnName, boolean expectedNullable)
        throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Column.class)).isTrue();
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(expectedColumnName);
        assertThat(column.nullable()).isEqualTo(expectedNullable);
    }

    private void assertJsonbField(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(JdbcTypeCode.class)).isTrue();
        assertThat(field.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.JSON);
    }

    private Set<String> fieldNames(Class<?> entityType) {
        return Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(Integer.class)
            || fieldType.equals(String.class)
            || fieldType.equals(Instant.class)
            || fieldType.equals(boolean.class)
            || fieldType.isEnum();
    }

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read persistence source: " + path, exception);
        }
    }
}
