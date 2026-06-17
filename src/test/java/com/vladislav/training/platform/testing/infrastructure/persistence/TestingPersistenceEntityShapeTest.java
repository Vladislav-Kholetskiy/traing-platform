package com.vladislav.training.platform.testing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет форму и состав {@code TestingPersistenceEntity}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class TestingPersistenceEntityShapeTest {

    @Test
    void testAttemptEntityShapeMatchesCanonicalPersistenceCarrier() throws NoSuchFieldException {
        assertEntityFieldSet(
            TestAttemptEntity.class,
            Set.of(
                "id",
                "userId",
                "testId",
                "assignmentTestId",
                "attemptMode",
                "status",
                "startedAt",
                "completedAt",
                "expiredAt",
                "abandonedAt",
                "lastActivityAt",
                "createdAt",
                "updatedAt"
            )
        );
        assertColumn(TestAttemptEntity.class, "userId", "user_id", false);
        assertColumn(TestAttemptEntity.class, "testId", "test_id", false);
        assertColumn(TestAttemptEntity.class, "assignmentTestId", "assignment_test_id", true);
        assertColumn(TestAttemptEntity.class, "startedAt", "started_at", false);
        assertColumn(TestAttemptEntity.class, "completedAt", "completed_at", true);
        assertColumn(TestAttemptEntity.class, "expiredAt", "expired_at", true);
        assertColumn(TestAttemptEntity.class, "abandonedAt", "abandoned_at", true);
        assertColumn(TestAttemptEntity.class, "lastActivityAt", "last_activity_at", false);
        assertColumn(TestAttemptEntity.class, "createdAt", "created_at", false);
        assertColumn(TestAttemptEntity.class, "updatedAt", "updated_at", false);

        Field attemptModeField = TestAttemptEntity.class.getDeclaredField("attemptMode");
        assertEnumField(attemptModeField, AttemptMode.class);
        assertColumn(TestAttemptEntity.class, "attemptMode", "attempt_mode", false);

        Field statusField = TestAttemptEntity.class.getDeclaredField("status");
        assertEnumField(statusField, TestAttemptStatus.class);
        assertColumn(TestAttemptEntity.class, "status", "status", false);

        assertThat(TestAttemptEntity.class.getAnnotation(Table.class).indexes())
            .extracting(index -> index.name())
            .containsExactlyInAnyOrder(
                "ix_test_att__user_id",
                "ix_test_att__test_id",
                "ix_test_att__asg_test_id",
                "ix_test_att__status",
                "ix_test_att__last_activity_at"
            );
    }

    @Test
    void userAnswerEntityShapeMatchesCanonicalAttemptQuestionAnchor() throws NoSuchFieldException {
        assertEntityFieldSet(
            UserAnswerEntity.class,
            Set.of("id", "testAttemptId", "questionId", "createdAt", "updatedAt")
        );
        assertColumn(UserAnswerEntity.class, "testAttemptId", "test_attempt_id", false);
        assertColumn(UserAnswerEntity.class, "questionId", "question_id", false);
        assertColumn(UserAnswerEntity.class, "createdAt", "created_at", false);
        assertColumn(UserAnswerEntity.class, "updatedAt", "updated_at", false);

        Table table = UserAnswerEntity.class.getAnnotation(Table.class);
        assertThat(table.indexes())
            .extracting(index -> index.name())
            .containsExactlyInAnyOrder("ix_usr_answer__test_att_id", "ix_usr_answer__question_id");
        assertThat(table.uniqueConstraints())
            .singleElement()
            .satisfies(unique -> {
                assertThat(unique.name()).isEqualTo("uq_usr_answer__test_att_id__question_id");
                assertThat(unique.columnNames()).containsExactly("test_attempt_id", "question_id");
            });
    }

    @Test
    void userAnswerItemEntityShapeMatchesCanonicalGranularPayloadCarrier() throws NoSuchFieldException {
        assertEntityFieldSet(
            UserAnswerItemEntity.class,
            Set.of(
                "id",
                "userAnswerId",
                "answerOptionId",
                "leftAnswerOptionId",
                "rightAnswerOptionId",
                "userOrderPosition",
                "createdAt",
                "updatedAt"
            )
        );
        assertColumn(UserAnswerItemEntity.class, "userAnswerId", "user_answer_id", false);
        assertColumn(UserAnswerItemEntity.class, "answerOptionId", "answer_option_id", true);
        assertColumn(UserAnswerItemEntity.class, "leftAnswerOptionId", "left_answer_option_id", true);
        assertColumn(UserAnswerItemEntity.class, "rightAnswerOptionId", "right_answer_option_id", true);
        assertColumn(UserAnswerItemEntity.class, "userOrderPosition", "user_order_position", true);
        assertColumn(UserAnswerItemEntity.class, "createdAt", "created_at", false);
        assertColumn(UserAnswerItemEntity.class, "updatedAt", "updated_at", false);

        assertThat(UserAnswerItemEntity.class.getAnnotation(Table.class).indexes())
            .extracting(index -> index.name())
            .containsExactlyInAnyOrder(
                "ix_usr_answer_item__usr_answer_id",
                "ix_usr_answer_item__ans_opt_id",
                "ix_usr_answer_item__left_ans_opt_id",
                "ix_usr_answer_item__right_ans_opt_id"
            );
    }

    @Test
    void entitiesRemainPlainPersistenceCarriersWithoutJpaRelations() {
        assertPlainPersistenceCarrier(TestAttemptEntity.class);
        assertPlainPersistenceCarrier(UserAnswerEntity.class);
        assertPlainPersistenceCarrier(UserAnswerItemEntity.class);
    }

    private void assertEntityFieldSet(Class<?> entityType, Set<String> expectedFieldNames) {
        assertThat(fieldNames(entityType)).isEqualTo(expectedFieldNames);
    }

    private void assertPlainPersistenceCarrier(Class<?> entityType) {
        boolean persistenceAnnotationsOnly = Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class)
                || field.isAnnotationPresent(Enumerated.class));
        boolean scalarFieldTypesOnly = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);
        boolean noRelationAnnotations = Stream.of(entityType.getDeclaredFields())
            .noneMatch(field -> field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(OneToOne.class));
        boolean noCollectionFields = Stream.of(entityType.getDeclaredFields())
            .map(Field::getType)
            .noneMatch(Collection.class::isAssignableFrom);

        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(persistenceAnnotationsOnly).isTrue();
        assertThat(scalarFieldTypesOnly).isTrue();
        assertThat(noRelationAnnotations).isTrue();
        assertThat(noCollectionFields).isTrue();
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(Integer.class)
            || fieldType.equals(Instant.class)
            || fieldType.isEnum();
    }

    private Set<String> fieldNames(Class<?> entityType) {
        return Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private void assertColumn(Class<?> entityType, String fieldName, String expectedColumnName, boolean expectedNullable)
        throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Column.class)).isTrue();
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(expectedColumnName);
        assertThat(column.nullable()).isEqualTo(expectedNullable);
    }

    private void assertEnumField(Field field, Class<?> expectedType) {
        assertThat(field.isAnnotationPresent(Enumerated.class)).isTrue();
        assertThat(field.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(field.getType()).isEqualTo(expectedType);
    }
}
