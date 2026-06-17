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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Проверяет форму и состав {@code AnalyticsCampaignAggregatePersistence}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class AnalyticsCampaignAggregatePersistenceShapeTest {

    private static final Path ENTITY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/AnalyticsCampaignAggregateEntity.java"
    );
    private static final Path REPOSITORY_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/SpringDataAnalyticsCampaignAggregateJpaRepository.java"
    );
    private static final Path ADAPTER_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaAnalyticsCampaignAggregateRepositoryAdapter.java"
    );

    @Test
    void entityMapsCanonicalAnalyticsCampaignAggregateTableAsPlainReadModelCarrier() throws NoSuchFieldException {
        assertThat(AnalyticsCampaignAggregateEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(AnalyticsCampaignAggregateEntity.class.getAnnotation(Table.class).name())
            .isEqualTo("analytics_campaign_aggregate");
        assertThat(fieldNames(AnalyticsCampaignAggregateEntity.class))
            .containsExactlyInAnyOrder(
                "id",
                "campaignId",
                "recipientSnapshotCount",
                "nonCancelledAssignmentsFromCampaignSnapshot",
                "completedAssignments",
                "overdueAssignments",
                "nonCancelledActivePool",
                "cancelledAssignments",
                "coveragePercent",
                "overduePercent",
                "calculatedAt",
                "refreshedAt",
                "reconciledAt"
            );

        assertColumn("campaignId", "campaign_id", false);
        assertColumn("recipientSnapshotCount", "recipient_snapshot_count", false);
        assertColumn(
            "nonCancelledAssignmentsFromCampaignSnapshot",
            "non_cancelled_assignments_from_campaign_snapshot",
            false
        );
        assertColumn("completedAssignments", "completed_assignments", false);
        assertColumn("overdueAssignments", "overdue_assignments", false);
        assertColumn("nonCancelledActivePool", "non_cancelled_active_pool", false);
        assertColumn("cancelledAssignments", "cancelled_assignments", false);
        assertColumn("coveragePercent", "coverage_percent", false);
        assertColumn("overduePercent", "overdue_percent", false);
        assertColumn("calculatedAt", "calculated_at", false);
        assertColumn("refreshedAt", "refreshed_at", false);
        assertColumn("reconciledAt", "reconciled_at", true);

        assertThat(plainPersistenceCarrier()).isTrue();
    }

    @Test
    void repositoryStaysPlainJpaRepositoryCarrierWithCanonicalCampaignKeyLookup() throws NoSuchFieldException {
        ParameterizedType jpaRepositoryType = Stream.of(SpringDataAnalyticsCampaignAggregateJpaRepository.class.getGenericInterfaces())
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .filter(type -> JpaRepository.class.equals(type.getRawType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("JpaRepository generic shape is missing"));

        Type[] typeArguments = jpaRepositoryType.getActualTypeArguments();

        assertThat(typeArguments).containsExactly(AnalyticsCampaignAggregateEntity.class, Long.class);
        assertThat(SpringDataAnalyticsCampaignAggregateJpaRepository.class.getDeclaredMethods())
            .singleElement()
            .satisfies(method -> {
                assertThat(method.getName()).isEqualTo("findByCampaignId");
                assertThat(method.getReturnType()).isEqualTo(Optional.class);
                assertThat(method.getParameterTypes()).containsExactly(Long.class);
            });
    }

    @Test
    void sourceBoundaryStaysFreeFromRefreshRebuildControllerServiceAndRecalculationDrift() throws Exception {
        String entitySource = Files.readString(ENTITY_SOURCE);
        String repositorySource = Files.readString(REPOSITORY_SOURCE);
        String adapterSource = Files.readString(ADAPTER_SOURCE);

        assertForbiddenDriftAbsent(entitySource);
        assertForbiddenDriftAbsent(repositorySource);
        assertForbiddenDriftAbsent(adapterSource);
        assertThat(entitySource).doesNotContain("import com.vladislav.training.platform");
        assertThat(repositorySource)
            .doesNotContain("service.")
            .doesNotContain("controller.")
            .doesNotContain("@Query");
        assertThat(adapterSource)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("controller.")
            .doesNotContain("service.");
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
        Field field = AnalyticsCampaignAggregateEntity.class.getDeclaredField(fieldName);
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
        boolean persistenceAnnotationsOnly = Stream.of(AnalyticsCampaignAggregateEntity.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .allMatch(field -> field.isAnnotationPresent(Column.class)
                || field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(GeneratedValue.class));
        boolean scalarFieldTypesOnly = Stream.of(AnalyticsCampaignAggregateEntity.class.getDeclaredFields())
            .map(Field::getType)
            .allMatch(this::isPlainPersistenceFieldType);
        boolean noCollectionFields = Stream.of(AnalyticsCampaignAggregateEntity.class.getDeclaredFields())
            .map(Field::getType)
            .noneMatch(Collection.class::isAssignableFrom);

        return persistenceAnnotationsOnly && scalarFieldTypesOnly && noCollectionFields;
    }

    private boolean isPlainPersistenceFieldType(Class<?> fieldType) {
        return fieldType.equals(Long.class)
            || fieldType.equals(Integer.class)
            || fieldType.equals(Instant.class)
            || fieldType.equals(BigDecimal.class);
    }
}
