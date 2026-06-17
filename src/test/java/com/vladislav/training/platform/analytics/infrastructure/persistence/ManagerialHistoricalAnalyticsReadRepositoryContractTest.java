package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialDepartmentTopicAnalyticsReadRow;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialUserTopicAnalyticsReadRow;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет договорённости вокруг {@code ManagerialHistoricalAnalyticsReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@ExtendWith(MockitoExtension.class)
class ManagerialHistoricalAnalyticsReadRepositoryContractTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-25T10:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");

    @Mock
    private EntityManager entityManager;

    @Test
    void criteriaRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsReadCriteria(
            null,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("managerialReadScope must not be null");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsReadCriteria(
            denyAllScope(),
            null,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodStart must not be null");

        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsReadCriteria(
            denyAllScope(),
            PERIOD_START,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodEnd must not be null");
    }

    @Test
    void criteriaRejectsInvalidPeriod() {
        assertThatThrownBy(() -> new ManagerialHistoricalAnalyticsReadCriteria(
            denyAllScope(),
            PERIOD_START,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be before periodEnd");
    }

    @Test
    void rowRecordsHaveExactExpectedShape() {
        assertThat(componentNames(ManagerialUserTopicAnalyticsReadRow.class))
            .containsExactly(
                "userId",
                "employeeNumber",
                "lastName",
                "firstName",
                "middleName",
                "topicId",
                "topicName",
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt"
            );

        assertThat(componentNames(ManagerialDepartmentTopicAnalyticsReadRow.class))
            .containsExactly(
                "organizationalUnitIdSnapshot",
                "organizationalUnitName",
                "organizationalPathSnapshot",
                "topicId",
                "topicName",
                "periodStart",
                "periodEnd",
                "averageScorePercent",
                "passRatePercent",
                "attemptCount",
                "errorCount",
                "calculatedAt",
                "refreshedAt"
            );
    }

    @Test
    void denyAllUserTopicCriteriaReturnsEmptyBeforeEntityManagerInteraction() {
        JpaManagerialHistoricalAnalyticsReadRepositoryAdapter adapter =
            new JpaManagerialHistoricalAnalyticsReadRepositoryAdapter(entityManager);

        List<ManagerialUserTopicAnalyticsReadRow> rows = adapter.findUserTopicRows(
            new ManagerialHistoricalAnalyticsReadCriteria(denyAllScope(), PERIOD_START, PERIOD_END)
        );

        assertThat(rows).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void denyAllDepartmentTopicCriteriaReturnsEmptyBeforeEntityManagerInteraction() {
        JpaManagerialHistoricalAnalyticsReadRepositoryAdapter adapter =
            new JpaManagerialHistoricalAnalyticsReadRepositoryAdapter(entityManager);

        List<ManagerialDepartmentTopicAnalyticsReadRow> rows = adapter.findDepartmentTopicRows(
            new ManagerialHistoricalAnalyticsReadCriteria(denyAllScope(), PERIOD_START, PERIOD_END)
        );

        assertThat(rows).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void adapterShapeStaysReadOnlyAndEntityManagerOnly() throws IOException {
        assertThat(ManagerialHistoricalAnalyticsReadRepository.class.isAssignableFrom(
            JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class
        )).isTrue();
        assertThat(JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class.getAnnotation(Transactional.class).readOnly())
            .isTrue();
        assertThat(fieldTypes(JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.class))
            .containsExactly(EntityManager.class);

        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaManagerialHistoricalAnalyticsReadRepositoryAdapter.java"
        );
        assertThat(source)
            .contains("AnalyticsUserTopicAggregateEntity")
            .contains("AnalyticsDepartmentTopicAggregateEntity")
            .contains("EntityManager")
            .contains("createQuery")
            .doesNotContain("ManagerialVisibleUsersRestrictionBuilder")
            .doesNotContain("UserOrgReadScopeJpaSupport")
            .doesNotContain("UserOrganizationAssignment")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("refresh(")
            .doesNotContain("refreshService")
            .doesNotContain("refreshAnalytics")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(")
            .doesNotContain("rebuild")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");
        assertThat(source)
            .containsAnyOf("refreshedAt", "\"freshedAt\"", "property(\"re\", \"freshedAt\")");
    }

    @Test
    void portSourceHasNoServiceOrControllerDependencies() throws IOException {
        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/repository/ManagerialHistoricalAnalyticsReadRepository.java"
        );

        assertThat(source)
            .doesNotContain("controller.")
            .doesNotContain("Controller")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService");
    }

    private ManagerialReadScope denyAllScope() {
        return new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.denyAll()
        );
    }

    private List<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
