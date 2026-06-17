package com.vladislav.training.platform.result.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.result.domain.ResultQuestionType;
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
import jakarta.persistence.UniqueConstraint;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет форму и состав {@code ResultPersistenceEntity}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class ResultPersistenceEntityShapeTest {

    @Test
    void resultEntityShapeMatchesImmutableHistoricalFactCarrier() throws NoSuchFieldException {
        assertEntityFieldSet(
            ResultEntity.class,
            Set.of(
                "id",
                "testAttemptId",
                "userIdSnapshot",
                "attemptMode",
                "assignmentId",
                "assignmentTestId",
                "testIdSnapshot",
                "testNameSnapshot",
                "thresholdPercent",
                "earnedScore",
                "maxScore",
                "scorePercent",
                "passed",
                "withinDeadline",
                "countedInAssignment",
                "scoringPolicyCode",
                "scoringPolicySnapshot",
                "completedAt",
                "organizationalUnitIdSnapshot",
                "organizationalPathSnapshot",
                "snapshotFinalTopicControlFlag",
                "createdAt"
            )
        );
        assertColumn(ResultEntity.class, "testAttemptId", "test_attempt_id", false);
        assertColumn(ResultEntity.class, "userIdSnapshot", "user_id_snapshot", false);
        assertColumn(ResultEntity.class, "assignmentId", "assignment_id", true);
        assertColumn(ResultEntity.class, "assignmentTestId", "assignment_test_id", true);
        assertColumn(ResultEntity.class, "testIdSnapshot", "test_id_snapshot", false);
        assertColumn(ResultEntity.class, "testNameSnapshot", "test_name_snapshot", false);
        assertColumn(ResultEntity.class, "thresholdPercent", "threshold_percent", false);
        assertColumn(ResultEntity.class, "earnedScore", "earned_score", false);
        assertColumn(ResultEntity.class, "maxScore", "max_score", false);
        assertColumn(ResultEntity.class, "scorePercent", "score_percent", false);
        assertColumn(ResultEntity.class, "withinDeadline", "within_deadline", true);
        assertColumn(ResultEntity.class, "countedInAssignment", "counted_in_assignment", true);
        assertColumn(ResultEntity.class, "scoringPolicyCode", "scoring_policy_code", false);
        assertColumn(ResultEntity.class, "scoringPolicySnapshot", "scoring_policy_snapshot", false);
        assertColumn(ResultEntity.class, "completedAt", "completed_at", false);
        assertColumn(ResultEntity.class, "organizationalUnitIdSnapshot", "organizational_unit_id_snapshot", false);
        assertColumn(ResultEntity.class, "organizationalPathSnapshot", "organizational_path_snapshot", false);
        assertColumn(ResultEntity.class, "snapshotFinalTopicControlFlag", "snapshot_final_topic_control_flag", false);
        assertColumn(ResultEntity.class, "createdAt", "created_at", false);
        assertUniqueConstraint(ResultEntity.class, "uq_result__test_att_id", "test_attempt_id");

        Field attemptModeField = ResultEntity.class.getDeclaredField("attemptMode");
        assertEnumField(attemptModeField, AttemptMode.class);
        assertColumn(ResultEntity.class, "attemptMode", "attempt_mode", false);
    }

    @Test
    void resultQuestionSnapshotEntityShapeMatchesSubordinateSnapshotCarrier() throws NoSuchFieldException {
        assertEntityFieldSet(
            ResultQuestionSnapshotEntity.class,
            Set.of(
                "id",
                "resultId",
                "questionOriginalId",
                "topicIdSnapshot",
                "body",
                "questionType",
                "displayOrder",
                "weight",
                "correctAnswerSnapshot",
                "userAnswerSnapshot",
                "earnedScore",
                "maxScore",
                "correct",
                "evaluationNote",
                "createdAt"
            )
        );
        assertColumn(ResultQuestionSnapshotEntity.class, "resultId", "result_id", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "questionOriginalId", "question_original_id", true);
        assertColumn(ResultQuestionSnapshotEntity.class, "topicIdSnapshot", "topic_id_snapshot", true);
        assertColumn(ResultQuestionSnapshotEntity.class, "body", "body", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "displayOrder", "display_order", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "weight", "weight", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "correctAnswerSnapshot", "correct_answer_snapshot", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "userAnswerSnapshot", "user_answer_snapshot", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "earnedScore", "earned_score", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "maxScore", "max_score", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "correct", "is_correct", false);
        assertColumn(ResultQuestionSnapshotEntity.class, "evaluationNote", "evaluation_note", true);
        assertColumn(ResultQuestionSnapshotEntity.class, "createdAt", "created_at", false);

        Field questionTypeField = ResultQuestionSnapshotEntity.class.getDeclaredField("questionType");
        assertEnumField(questionTypeField, ResultQuestionType.class);
        assertColumn(ResultQuestionSnapshotEntity.class, "questionType", "question_type", false);
    }

    @Test
    void resultAnswerOptionSnapshotEntityShapeMatchesOptionLevelSnapshotCarrier() throws NoSuchFieldException {
        assertEntityFieldSet(
            ResultAnswerOptionSnapshotEntity.class,
            Set.of(
                "id",
                "resultQuestionSnapshotId",
                "answerOptionOriginalId",
                "body",
                "displayOrder",
                "correctAtSnapshot",
                "selectedByUser",
                "createdAt"
            )
        );
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "resultQuestionSnapshotId", "result_question_snapshot_id", false);
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "answerOptionOriginalId", "answer_option_original_id", true);
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "body", "body", false);
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "displayOrder", "display_order", false);
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "correctAtSnapshot", "is_correct_at_snapshot", false);
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "selectedByUser", "is_selected_by_user", false);
        assertColumn(ResultAnswerOptionSnapshotEntity.class, "createdAt", "created_at", false);
    }

    @Test
    void entitiesRemainPlainPersistenceCarriersWithoutJpaRelations() {
        assertPlainPersistenceCarrier(ResultEntity.class);
        assertPlainPersistenceCarrier(ResultQuestionSnapshotEntity.class);
        assertPlainPersistenceCarrier(ResultAnswerOptionSnapshotEntity.class);
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
            || fieldType.equals(Boolean.class)
            || fieldType.equals(String.class)
            || fieldType.equals(Instant.class)
            || fieldType.equals(BigDecimal.class)
            || fieldType.equals(boolean.class)
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

    private void assertUniqueConstraint(Class<?> entityType, String expectedConstraintName, String... expectedColumnNames) {
        assertThat(entityType.isAnnotationPresent(Table.class)).isTrue();
        Table table = entityType.getAnnotation(Table.class);
        assertThat(table.uniqueConstraints())
            .extracting(UniqueConstraint::name, UniqueConstraint::columnNames)
            .contains(org.assertj.core.groups.Tuple.tuple(expectedConstraintName, expectedColumnNames));
    }
}
