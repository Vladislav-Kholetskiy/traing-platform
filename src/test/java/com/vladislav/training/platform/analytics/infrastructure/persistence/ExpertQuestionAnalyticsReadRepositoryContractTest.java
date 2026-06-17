package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
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
 * Проверяет договорённости вокруг {@code ExpertQuestionAnalyticsReadRepository}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@ExtendWith(MockitoExtension.class)
class ExpertQuestionAnalyticsReadRepositoryContractTest {

    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");

    @Mock
    private EntityManager entityManager;

    @Test
    void criteriaRejectsNullMandatoryFields() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadCriteria(
            null,
            PERIOD_START,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("accessReadScope must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadCriteria(
            AccessReadScope.fullAccess(),
            null,
            PERIOD_END
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodStart must not be null");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadCriteria(
            AccessReadScope.fullAccess(),
            PERIOD_START,
            null
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("periodEnd must not be null");
    }

    @Test
    void criteriaRejectsInvalidPeriod() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadCriteria(
            AccessReadScope.fullAccess(),
            PERIOD_START,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadCriteria(
            AccessReadScope.fullAccess(),
            PERIOD_END,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");
    }

    @Test
    void readRowHasExactExpectedShapeAndRejectsNullMandatoryFields() {
        ExpertQuestionAnalyticsReadRow row = new ExpertQuestionAnalyticsReadRow(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            new BigDecimal("4.7500"),
            Instant.parse("2026-05-01T00:10:00Z"),
            Instant.parse("2026-05-01T00:15:00Z")
        );

        assertThat(row.questionId()).isEqualTo(701L);
        assertThat(componentNames(ExpertQuestionAnalyticsReadRow.class))
            .containsExactly(
                "questionId",
                "periodStart",
                "periodEnd",
                "attemptCount",
                "correctCount",
                "incorrectCount",
                "averageEarnedScore",
                "calculatedAt",
                "refreshedAt"
            );

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsReadRow(
            null,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            3,
            new BigDecimal("4.7500"),
            Instant.parse("2026-05-01T00:10:00Z"),
            Instant.parse("2026-05-01T00:15:00Z")
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("questionId must not be null");
    }

    @Test
    void denyAllScopeReturnsEmptyBeforeAnyEntityManagerInteraction() {
        JpaExpertQuestionAnalyticsReadRepositoryAdapter adapter =
            new JpaExpertQuestionAnalyticsReadRepositoryAdapter(entityManager);

        List<ExpertQuestionAnalyticsReadRow> rows = adapter.findQuestionAnalyticsRows(
            new ExpertQuestionAnalyticsReadCriteria(AccessReadScope.denyAll(), PERIOD_START, PERIOD_END)
        );

        assertThat(rows).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void scopedAccessReturnsEmptyBeforeAnyEntityManagerInteraction() {
        JpaExpertQuestionAnalyticsReadRepositoryAdapter adapter =
            new JpaExpertQuestionAnalyticsReadRepositoryAdapter(entityManager);

        List<ExpertQuestionAnalyticsReadRow> rows = adapter.findQuestionAnalyticsRows(
            new ExpertQuestionAnalyticsReadCriteria(
                AccessReadScope.scoped(Set.of(42L), Set.of("/company/division")),
                PERIOD_START,
                PERIOD_END
            )
        );

        assertThat(rows).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void adapterShapeStaysReadOnlyAndEntityManagerOnly() throws IOException {
        assertThat(ExpertQuestionAnalyticsReadRepository.class.isAssignableFrom(
            JpaExpertQuestionAnalyticsReadRepositoryAdapter.class
        )).isTrue();
        assertThat(JpaExpertQuestionAnalyticsReadRepositoryAdapter.class.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(JpaExpertQuestionAnalyticsReadRepositoryAdapter.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(JpaExpertQuestionAnalyticsReadRepositoryAdapter.class.getAnnotation(Transactional.class).readOnly())
            .isTrue();
        assertThat(fieldTypes(JpaExpertQuestionAnalyticsReadRepositoryAdapter.class))
            .containsExactly(EntityManager.class);

        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaExpertQuestionAnalyticsReadRepositoryAdapter.java"
        );
        assertThat(source)
            .contains("AnalyticsQuestionAggregateEntity")
            .contains("EntityManager")
            .contains("createQuery")
            .contains("periodStart")
            .contains("periodEnd")
            .contains("lessThan")
            .contains("greaterThan")
            .doesNotContain("AnalyticsQueryService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("ManagerialReadScope")
            .doesNotContain("ManagerialReadScopeProjectionService")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("ManagerialVisibleUsersRestrictionBuilder")
            .doesNotContain("UserOrgReadScopeJpaSupport")
            .doesNotContain("Controller")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");
    }

    @Test
    void portSourceHasNoServiceControllerOrWriteDependencies() throws IOException {
        String source = read(
            "src/main/java/com/vladislav/training/platform/analytics/repository/ExpertQuestionAnalyticsReadRepository.java"
        );

        assertThat(source)
            .doesNotContain("AnalyticsQueryService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("EntityManager")
            .doesNotContain("Controller")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");
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
