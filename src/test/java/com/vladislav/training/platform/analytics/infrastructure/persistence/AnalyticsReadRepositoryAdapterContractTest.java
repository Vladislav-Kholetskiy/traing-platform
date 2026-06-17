package com.vladislav.training.platform.analytics.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsDepartmentTopicAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsQuestionAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsUserTopicAggregateRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет договорённости вокруг {@code AnalyticsReadRepositoryAdapter}.
 * Тест помогает сохранить предсказуемое поведение.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsReadRepositoryAdapterContractTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T18:00:00Z");

    @Mock
    private SpringDataAnalyticsUserTopicAggregateJpaRepository userRepository;
    @Mock
    private SpringDataAnalyticsDepartmentTopicAggregateJpaRepository departmentRepository;
    @Mock
    private SpringDataAnalyticsQuestionAggregateJpaRepository questionRepository;

    @Test
    void adaptersStayReadOnlyRepositoryAnnotatedAndBoundOnlyToTheirSpringDataCarrier() {
        assertAdapterShape(
            JpaAnalyticsUserTopicAggregateRepositoryAdapter.class,
            AnalyticsUserTopicAggregateRepository.class,
            Set.of(SpringDataAnalyticsUserTopicAggregateJpaRepository.class)
        );
        assertAdapterShape(
            JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter.class,
            AnalyticsDepartmentTopicAggregateRepository.class,
            Set.of(SpringDataAnalyticsDepartmentTopicAggregateJpaRepository.class)
        );
        assertAdapterShape(
            JpaAnalyticsQuestionAggregateRepositoryAdapter.class,
            AnalyticsQuestionAggregateRepository.class,
            Set.of(SpringDataAnalyticsQuestionAggregateJpaRepository.class)
        );
    }

    @Test
    void userTopicAdapterMapsEntityFieldsIntoDomainAndUsesOnlyReadMethods() {
        JpaAnalyticsUserTopicAggregateRepositoryAdapter adapter = new JpaAnalyticsUserTopicAggregateRepositoryAdapter(userRepository);
        AnalyticsUserTopicAggregateEntity entity = userEntity();

        when(userRepository.findById(11L)).thenReturn(java.util.Optional.of(entity));
        when(userRepository.findAll()).thenReturn(List.of(entity));

        AnalyticsUserTopicAggregate byId = adapter.findUserTopicAggregateById(11L);
        AnalyticsUserTopicAggregate byKey = adapter.findUserTopicAggregate(101L, 501L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600));
        List<AnalyticsUserTopicAggregate> byUser = adapter.findUserTopicAggregatesByUserId(101L);
        List<AnalyticsUserTopicAggregate> byTopic = adapter.findUserTopicAggregatesByTopicId(501L);

        assertThat(byId.id()).isEqualTo(11L);
        assertThat(byId.lastAssignedFinalResultId()).isEqualTo(901L);
        assertThat(byId.lastAssignedFinalScorePercent()).isEqualByComparingTo("88.5000");
        assertThat(byId.reconciledAt()).isEqualTo(FIXED_INSTANT.plusSeconds(180));
        assertThat(byKey).isEqualTo(byId);
        assertThat(byUser).containsExactly(byId);
        assertThat(byTopic).containsExactly(byId);

        verify(userRepository).findById(11L);
        verify(userRepository, times(3)).findAll();
    }

    @Test
    void departmentTopicAdapterMapsEntityFieldsIntoDomainAndUsesOnlyReadMethods() {
        JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter adapter =
            new JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter(departmentRepository);
        AnalyticsDepartmentTopicAggregateEntity entity = departmentEntity();

        when(departmentRepository.findById(21L)).thenReturn(java.util.Optional.of(entity));
        when(departmentRepository.findAll()).thenReturn(List.of(entity));

        AnalyticsDepartmentTopicAggregate byId = adapter.findDepartmentTopicAggregateById(21L);
        AnalyticsDepartmentTopicAggregate byKey = adapter.findDepartmentTopicAggregate(
            301L,
            501L,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(3600)
        );
        List<AnalyticsDepartmentTopicAggregate> byOrg = adapter.findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(301L);

        assertThat(byId.id()).isEqualTo(21L);
        assertThat(byId.organizationalPathSnapshot()).isEqualTo("/company/division/unit");
        assertThat(byId.averageScorePercent()).isEqualByComparingTo("78.2500");
        assertThat(byId.reconciledAt()).isEqualTo(FIXED_INSTANT.plusSeconds(240));
        assertThat(byKey).isEqualTo(byId);
        assertThat(byOrg).containsExactly(byId);

        verify(departmentRepository).findById(21L);
        verify(departmentRepository, times(2)).findAll();
    }

    @Test
    void questionAdapterMapsEntityFieldsIntoDomainAndUsesOnlyReadMethods() {
        JpaAnalyticsQuestionAggregateRepositoryAdapter adapter = new JpaAnalyticsQuestionAggregateRepositoryAdapter(questionRepository);
        AnalyticsQuestionAggregateEntity entity = questionEntity();

        when(questionRepository.findById(31L)).thenReturn(java.util.Optional.of(entity));
        when(questionRepository.findAll()).thenReturn(List.of(entity));

        AnalyticsQuestionAggregate byId = adapter.findQuestionAggregateById(31L);
        AnalyticsQuestionAggregate byKey = adapter.findQuestionAggregate(701L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600));
        List<AnalyticsQuestionAggregate> byQuestion = adapter.findQuestionAggregatesByQuestionId(701L);

        assertThat(byId.id()).isEqualTo(31L);
        assertThat(byId.correctCount()).isEqualTo(8);
        assertThat(byId.incorrectCount()).isEqualTo(3);
        assertThat(byId.averageEarnedScore()).isEqualByComparingTo("4.7500");
        assertThat(byId.reconciledAt()).isEqualTo(FIXED_INSTANT.plusSeconds(300));
        assertThat(byKey).isEqualTo(byId);
        assertThat(byQuestion).containsExactly(byId);

        verify(questionRepository).findById(31L);
        verify(questionRepository, times(2)).findAll();
    }

    @Test
    void writeMethodsFailClosedForAnalyticsReadOnlyAdapters() {
        JpaAnalyticsUserTopicAggregateRepositoryAdapter userAdapter = new JpaAnalyticsUserTopicAggregateRepositoryAdapter(userRepository);
        JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter departmentAdapter =
            new JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter(departmentRepository);
        JpaAnalyticsQuestionAggregateRepositoryAdapter questionAdapter =
            new JpaAnalyticsQuestionAggregateRepositoryAdapter(questionRepository);

        assertThatThrownBy(() -> userAdapter.saveUserTopicAggregate(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("только чтение");
        assertThatThrownBy(userAdapter::deleteAllUserTopicAggregates)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("только чтение");

        assertThatThrownBy(() -> departmentAdapter.saveDepartmentTopicAggregate(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("только чтение");
        assertThatThrownBy(departmentAdapter::deleteAllDepartmentTopicAggregates)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("только чтение");

        assertThatThrownBy(() -> questionAdapter.saveQuestionAggregate(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("только чтение");
        assertThatThrownBy(questionAdapter::deleteAllQuestionAggregates)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("только чтение");
    }

    @Test
    void adapterSourcesStayFreeFromRefreshRebuildQuestionServicesAndWriteCalls() throws Exception {
        assertForbiddenSourceDrift(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaAnalyticsUserTopicAggregateRepositoryAdapter.java"
        );
        assertForbiddenSourceDrift(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaAnalyticsDepartmentTopicAggregateRepositoryAdapter.java"
        );
        assertForbiddenSourceDrift(
            "src/main/java/com/vladislav/training/platform/analytics/infrastructure/persistence/JpaAnalyticsQuestionAggregateRepositoryAdapter.java"
        );
    }

    private void assertForbiddenSourceDrift(String path) throws Exception {
        String source = Files.readString(Path.of(path));
        assertThat(source)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("controller.")
            .doesNotContain("service.")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".flush(");
    }

    private void assertAdapterShape(Class<?> adapterClass, Class<?> contractClass, Set<Class<?>> expectedFieldTypes) {
        assertThat(contractClass.isAssignableFrom(adapterClass)).isTrue();
        assertThat(adapterClass.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(adapterClass.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(adapterClass.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(fieldTypes(adapterClass)).isEqualTo(expectedFieldTypes);
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields()).map(Field::getType).collect(Collectors.toUnmodifiableSet());
    }

    private AnalyticsUserTopicAggregateEntity userEntity() {
        AnalyticsUserTopicAggregateEntity entity = new AnalyticsUserTopicAggregateEntity();
        entity.setId(11L);
        entity.setUserId(101L);
        entity.setTopicId(501L);
        entity.setPeriodStart(FIXED_INSTANT);
        entity.setPeriodEnd(FIXED_INSTANT.plusSeconds(3600));
        entity.setLastAssignedFinalResultId(901L);
        entity.setLastAssignedFinalCompletedAt(FIXED_INSTANT.plusSeconds(60));
        entity.setLastAssignedFinalScorePercent(new BigDecimal("88.5000"));
        entity.setLastAssignedFinalPassed(true);
        entity.setAverageScorePercent(new BigDecimal("81.2500"));
        entity.setPassRatePercent(new BigDecimal("66.6700"));
        entity.setAttemptCount(12);
        entity.setErrorCount(4);
        entity.setCalculatedAt(FIXED_INSTANT.plusSeconds(120));
        entity.setRefreshedAt(FIXED_INSTANT.plusSeconds(150));
        entity.setReconciledAt(FIXED_INSTANT.plusSeconds(180));
        return entity;
    }

    private AnalyticsDepartmentTopicAggregateEntity departmentEntity() {
        AnalyticsDepartmentTopicAggregateEntity entity = new AnalyticsDepartmentTopicAggregateEntity();
        entity.setId(21L);
        entity.setOrganizationalUnitIdSnapshot(301L);
        entity.setOrganizationalPathSnapshot("/company/division/unit");
        entity.setTopicId(501L);
        entity.setPeriodStart(FIXED_INSTANT);
        entity.setPeriodEnd(FIXED_INSTANT.plusSeconds(3600));
        entity.setAverageScorePercent(new BigDecimal("78.2500"));
        entity.setPassRatePercent(new BigDecimal("61.5000"));
        entity.setAttemptCount(20);
        entity.setErrorCount(7);
        entity.setCalculatedAt(FIXED_INSTANT.plusSeconds(200));
        entity.setRefreshedAt(FIXED_INSTANT.plusSeconds(220));
        entity.setReconciledAt(FIXED_INSTANT.plusSeconds(240));
        return entity;
    }

    private AnalyticsQuestionAggregateEntity questionEntity() {
        AnalyticsQuestionAggregateEntity entity = new AnalyticsQuestionAggregateEntity();
        entity.setId(31L);
        entity.setQuestionId(701L);
        entity.setPeriodStart(FIXED_INSTANT);
        entity.setPeriodEnd(FIXED_INSTANT.plusSeconds(3600));
        entity.setAttemptCount(15);
        entity.setCorrectCount(8);
        entity.setIncorrectCount(3);
        entity.setAverageEarnedScore(new BigDecimal("4.7500"));
        entity.setCalculatedAt(FIXED_INSTANT.plusSeconds(260));
        entity.setRefreshedAt(FIXED_INSTANT.plusSeconds(280));
        entity.setReconciledAt(FIXED_INSTANT.plusSeconds(300));
        return entity;
    }
}
