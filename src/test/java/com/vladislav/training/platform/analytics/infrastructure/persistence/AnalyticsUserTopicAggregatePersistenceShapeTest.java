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
 * Проверяет форму и состав {@code AnalyticsUserTopicAggregatePersistence}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class AnalyticsUserTopicAggregatePersistenceShapeTest {

    private static final Path ENTITY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/AnalyticsUserTopicAggregateEntity.java"
    );
    private static final Path REPOSITORY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/SpringDataAnalyticsUserTopicAggregateJpaRepository.java"
    );

    @Test
    void entityMapsCanonicalAnalyticsUserTopicAggregateTableAsPlainReadModelCarrier() throws NoSuchFieldException {
        assertThat(AnalyticsUserTopicAggregateEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(AnalyticsUserTopicAggregateEntity.class.getAnnotation(Table.class).name())
            .isEqualTo("analytics_user_topic_aggregate");
        assertThat(fieldNames(AnalyticsUserTopicAggregateEntity.class))
            .containsExactlyInAnyOrder(
                "id",
                "userId",
                "topicId",
                "periodStart",
                "periodEnd",
                "lastAssignedFinalResultId",
                "lastAssignedFinalCompletedAt",
                "lastAssignedFinalScorePercent",
                "lastAssignedFinalPassed",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt",
                "reconciledAt"
            );

        assertColumn("userId", "user_id", false);
        assertColumn("topicId", "topic_id", false);
        assertColumn("periodStart", "period_start", false);
        assertColumn("periodEnd", "period_end", false);
        assertColumn("lastAssignedFinalResultId", "last_assigned_final_result_id", true);
        assertColumn("lastAssignedFinalCompletedAt", "last_assigned_final_completed_at", true);
        assertColumn("lastAssignedFinalScorePercent", "last_assigned_final_score_percent", true);
        assertColumn("lastAssignedFinalPassed", "last_assigned_final_passed", true);
        assertColumn("averageScorePercent", "average_score_percent", false);
        assertColumn("passRatePercent", "pass_rate_percent", false);
        assertColumn("attemptCount", "attempt_count", false);
        assertColumn("errorCount", "error_count", false);
        assertColumn("calculatedAt", "calculated_at", false);
        assertColumn("refreshedAt", "refreshed_at", false);
        assertColumn("reconciledAt", "reconciled_at", true);

        assertThat(plainPersistenceCarrier()).isTrue();
    }

    @Test
    void repositoryRemainsPlainJpaRepositoryCarrierWithoutCustomQueries() {
        ParameterizedType jpaRepositoryType = Stream.of(SpringDataAnalyticsUserTopicAggregateJpaRepository.class.getGenericInterfaces())
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .filter(type -> JpaRepository.class.equals(type.getRawType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("JpaRepository generic shape is missing"));

        Type[] typeArguments = jpaRepositoryType.getActualTypeArguments();

        assertThat(typeArguments).containsExactly(AnalyticsUserTopicAggregateEntity.class, Long.class);
        assertThat(SpringDataAnalyticsUserTopicAggregateJpaRepository.class.getDeclaredMethods()).isEmpty();
    }

    @Test
    void sourceBoundaryStaysFreeFromRefreshRebuildRecordingAndServiceControllerDrift() throws Exception {
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
            .doesNotContain("Controller")
            .doesNotContain("Service");
    }

    private void assertColumn(String fieldName, String expectedColumnName, boolean expectedNullable) throws NoSuchFieldException {
        Field field = AnalyticsUserTopicAggregateEntity.class.getDeclaredField(fieldName);
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
        boolean persistenceAnnotationsOnly = Stream.of(AnalyticsUserTopicAggregateEntity.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class));
        boolean scalarFieldTypesOnly = Stream.of(AnalyticsUserTopicAggregateEntity.class.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);
        boolean noCollectionFields = Stream.of(AnalyticsUserTopicAggregateEntity.class.getDeclaredFields())
            .map(Field::getType)
            .noneMatch(Collection.class::isAssignableFrom);

        return persistenceAnnotationsOnly && scalarFieldTypesOnly && noCollectionFields;
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(Integer.class)
            || fieldType.equals(Boolean.class)
            || fieldType.equals(Instant.class)
            || fieldType.equals(BigDecimal.class);
    }
}
