package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Проверяет форму и состав {@code AnalyticsQuestionAggregatePersistence}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class AnalyticsQuestionAggregatePersistenceShapeTest {

    private static final Path ENTITY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/AnalyticsQuestionAggregateEntity.java"
    );
    private static final Path REPOSITORY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/SpringDataAnalyticsQuestionAggregateJpaRepository.java"
    );

    @Test
    void entityMapsCanonicalAnalyticsQuestionAggregateTableAsPlainReadModelCarrier() throws NoSuchFieldException {
        assertThat(AnalyticsQuestionAggregateEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(AnalyticsQuestionAggregateEntity.class.getAnnotation(Table.class).name())
            .isEqualTo("analytics_question_aggregate");
        assertThat(fieldNames(AnalyticsQuestionAggregateEntity.class))
            .containsExactlyInAnyOrder(
                "id",
                "questionId",
                "periodStart",
                "periodEnd",
                "attemptCount",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "calculatedAt",
                "refreshedAt",
                "reconciledAt"
            );

        assertColumn("questionId", "question_id", false);
        assertColumn("periodStart", "period_start", false);
        assertColumn("periodEnd", "period_end", false);
        assertColumn("attemptCount", "attempt_count", false);
        assertColumn("correctCount", "correct_count", false);
        assertColumn("incorrectCount", "incorrect_count", false);
        assertColumn("averageEarnedScore", "average_earned_score", false);
        assertColumn("calculatedAt", "calculated_at", false);
        assertColumn("refreshedAt", "refreshed_at", false);
        assertColumn("reconciledAt", "reconciled_at", true);

        assertThat(plainPersistenceCarrier()).isTrue();
    }

    @Test
    void repositoryRemainsPlainJpaRepositoryCarrierWithoutCustomQueries() {
        ParameterizedType jpaRepositoryType = Stream.of(SpringDataAnalyticsQuestionAggregateJpaRepository.class.getGenericInterfaces())
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .filter(type -> JpaRepository.class.equals(type.getRawType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("JpaRepository generic shape is missing"));

        Type[] typeArguments = jpaRepositoryType.getActualTypeArguments();

        assertThat(typeArguments).containsExactly(AnalyticsQuestionAggregateEntity.class, Long.class);
        assertThat(SpringDataAnalyticsQuestionAggregateJpaRepository.class.getDeclaredMethods()).isEmpty();
    }

    @Test
    void sourceBoundaryStaysFreeFromRefreshRebuildRecordingQuestionServicesAndControllerDrift() throws Exception {
        String entitySource = Files.readString(ENTITY_SOURCE);
        String repositorySource = Files.readString(REPOSITORY_SOURCE);

        assertForbiddenDriftAbsent(entitySource);
        assertForbiddenDriftAbsent(repositorySource);
        assertThat(entitySource).doesNotContain("import com.vladislav.training.platform");
        assertThat(repositorySource)
            .doesNotContain("service.")
            .doesNotContain("controller.")
            .doesNotContain("@Query");
    }

    private void assertForbiddenDriftAbsent(String source) {
        assertThat(source)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("Controller")
            .doesNotContain("Service");
    }

    private void assertColumn(String fieldName, String expectedColumnName, boolean expectedNullable) throws NoSuchFieldException {
        Field field = AnalyticsQuestionAggregateEntity.class.getDeclaredField(fieldName);
        assertThat(field.isAnnotationPresent(Column.class)).isTrue();
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(expectedColumnName);
        assertThat(column.nullable()).isEqualTo(expectedNullable);
    }

    private Set<String> fieldNames(Class<?> entityType) {
        return Stream.of(entityType.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private boolean plainPersistenceCarrier() {
        boolean persistenceAnnotationsOnly = Stream.of(AnalyticsQuestionAggregateEntity.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class));
        boolean scalarFieldTypesOnly = Stream.of(AnalyticsQuestionAggregateEntity.class.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);
        boolean noCollectionFields = Stream.of(AnalyticsQuestionAggregateEntity.class.getDeclaredFields())
            .map(Field::getType)
            .noneMatch(Collection.class::isAssignableFrom);

        return persistenceAnnotationsOnly && scalarFieldTypesOnly && noCollectionFields;
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(Integer.class)
            || fieldType.equals(Boolean.class)
            || fieldType.equals(String.class)
            || fieldType.equals(Instant.class)
            || fieldType.equals(BigDecimal.class);
    }
}
