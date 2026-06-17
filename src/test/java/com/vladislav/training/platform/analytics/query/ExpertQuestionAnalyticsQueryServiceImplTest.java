package com.vladislav.training.platform.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadRow;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.controller.ContentLifecycleController;
import com.vladislav.training.platform.content.service.QuestionCommandService;
import com.vladislav.training.platform.content.service.QuestionLifecycleService;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import java.lang.reflect.Field;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет поведение {@code ExpertQuestionAnalyticsQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ExpertQuestionAnalyticsQueryServiceImplTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-28T13:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-05-01T09:00:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-05-01T09:15:00Z");

    @Mock
    private ExpertQuestionAnalyticsReadRepository expertQuestionAnalyticsReadRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;

    @Test
    void serviceIsReadOnlyAndDependsOnlyOnRepositoryAndAccessSpecificationPolicy() {
        assertThat(ExpertQuestionAnalyticsQueryService.class).isAssignableFrom(ExpertQuestionAnalyticsQueryServiceImpl.class);
        assertThat(ExpertQuestionAnalyticsQueryServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ExpertQuestionAnalyticsQueryServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(ExpertQuestionAnalyticsQueryServiceImpl.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(fieldTypes(ExpertQuestionAnalyticsQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                ExpertQuestionAnalyticsReadRepository.class,
                AccessSpecificationPolicy.class
            );
    }

    @Test
    void findQuestionAnalyticsResolvesExpertQuestionAnalyticsScopeAndMapsRows() {
        ExpertQuestionAnalyticsQueryServiceImpl service = service();
        ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery query = validQuery();
        AccessReadScope accessReadScope = AccessReadScope.fullAccess();
        ExpertQuestionAnalyticsReadRow row = new ExpertQuestionAnalyticsReadRow(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            7,
            new BigDecimal("4.7500"),
            CALCULATED_AT,
            REFRESHED_AT
        );
        when(accessSpecificationPolicy.resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class)))
            .thenReturn(accessReadScope);
        when(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            org.mockito.ArgumentMatchers.any(ExpertQuestionAnalyticsReadCriteria.class)
        )).thenReturn(List.of(row));

        List<ExpertQuestionAnalyticsDto> result = service.findQuestionAnalytics(query);

        assertThat(result).containsExactly(new ExpertQuestionAnalyticsDto(
            701L,
            PERIOD_START,
            PERIOD_END,
            15,
            8,
            7,
            new BigDecimal("4.7500"),
            CALCULATED_AT,
            REFRESHED_AT
        ));

        ArgumentCaptor<AccessPolicyQueryContext> contextCaptor = ArgumentCaptor.forClass(AccessPolicyQueryContext.class);
        ArgumentCaptor<ExpertQuestionAnalyticsReadCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(ExpertQuestionAnalyticsReadCriteria.class);
        var ordered = inOrder(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
        ordered.verify(accessSpecificationPolicy).resolveReadScope(contextCaptor.capture());
        ordered.verify(expertQuestionAnalyticsReadRepository).findQuestionAnalyticsRows(criteriaCaptor.capture());

        AccessPolicyQueryContext context = contextCaptor.getValue();
        assertThat(context.actorUserId()).isEqualTo(101L);
        assertThat(context.contour()).isEqualTo(AccessReadArea.EXPERT_QUESTION_ANALYTICS);
        assertThat(context.readType()).isEqualTo(AccessReadType.ANALYTICS);
        assertThat(context.effectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(context.targetUserId()).isNull();
        assertThat(context.targetOrganizationalUnitId()).isNull();
        assertThat(context.targetEntityFamily()).isEqualTo("expert_question_analytics");
        assertThat(context.subjectScope()).isEqualTo(AccessReadSubjectScope.UNSPECIFIED);
        assertThat(context.subjectSemantics()).isEqualTo(AccessReadSubjectSemantics.EXPERT);

        ExpertQuestionAnalyticsReadCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.accessReadScope()).isEqualTo(accessReadScope);
        assertThat(criteria.periodStart()).isEqualTo(PERIOD_START);
        assertThat(criteria.periodEnd()).isEqualTo(PERIOD_END);
        verifyNoMoreInteractions(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
    }

    @Test
    void denyPathFailsClosedBeforeRepositoryMaterialization() {
        when(accessSpecificationPolicy.resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class)))
            .thenReturn(AccessReadScope.denyAll());

        assertThatThrownBy(() -> service().findQuestionAnalytics(validQuery()))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("expert question analytics");

        verify(accessSpecificationPolicy).resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class));
        verifyNoInteractions(expertQuestionAnalyticsReadRepository);
        verifyNoMoreInteractions(accessSpecificationPolicy);
    }

    @Test
    void policyReturnsNullScopeFailsClosedBeforeRepositoryMaterialization() {
        when(accessSpecificationPolicy.resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> service().findQuestionAnalytics(validQuery()))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("expert question analytics");

        verify(accessSpecificationPolicy).resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class));
        verifyNoInteractions(expertQuestionAnalyticsReadRepository);
        verifyNoMoreInteractions(accessSpecificationPolicy);
    }

    @Test
    void emptyRepositoryResponseIsReturnedAsEmptyList() {
        when(accessSpecificationPolicy.resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class)))
            .thenReturn(AccessReadScope.fullAccess());
        when(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            org.mockito.ArgumentMatchers.any(ExpertQuestionAnalyticsReadCriteria.class)
        )).thenReturn(List.of());

        assertThat(service().findQuestionAnalytics(validQuery())).isEmpty();
        var ordered = inOrder(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
        ordered.verify(accessSpecificationPolicy).resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class));
        ordered.verify(expertQuestionAnalyticsReadRepository).findQuestionAnalyticsRows(
            org.mockito.ArgumentMatchers.any(ExpertQuestionAnalyticsReadCriteria.class)
        );
        verifyNoMoreInteractions(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
    }

    @Test
    void repositoryFailureIsPropagatedWithoutFakeEmptyFallback() {
        RuntimeException failure = new RuntimeException("repository failure");
        when(accessSpecificationPolicy.resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class)))
            .thenReturn(AccessReadScope.fullAccess());
        when(expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(
            org.mockito.ArgumentMatchers.any(ExpertQuestionAnalyticsReadCriteria.class)
        )).thenThrow(failure);

        assertThatThrownBy(() -> service().findQuestionAnalytics(validQuery()))
            .isSameAs(failure);

        var ordered = inOrder(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
        ordered.verify(accessSpecificationPolicy).resolveReadScope(org.mockito.ArgumentMatchers.any(AccessPolicyQueryContext.class));
        ordered.verify(expertQuestionAnalyticsReadRepository).findQuestionAnalyticsRows(
            org.mockito.ArgumentMatchers.any(ExpertQuestionAnalyticsReadCriteria.class)
        );
        verifyNoMoreInteractions(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
    }

    @Test
    void findQuestionAnalyticsRejectsNullQuery() {
        assertThatThrownBy(() -> service().findQuestionAnalytics(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("query must not be null");

        verifyNoInteractions(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
    }

    @Test
    void invalidPeriodsFailFastAtQueryContractBeforePolicyAndRepositoryRead() {
        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_START,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");

        assertThatThrownBy(() -> new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_END,
            PERIOD_START
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("periodStart must be strictly before periodEnd");

        verifyNoInteractions(accessSpecificationPolicy, expertQuestionAnalyticsReadRepository);
    }

    @Test
    void sourceLevelAntiDriftGuardRejectsGenericAnalyticsMutationPersistenceAndManagerialDependencies()
        throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/query/ExpertQuestionAnalyticsQueryServiceImpl.java"
        ));

        assertThat(source)
            .contains("AccessSpecificationPolicy")
            .contains("AccessReadArea.EXPERT_QUESTION_ANALYTICS")
            .contains("AccessReadType.ANALYTICS")
            .contains("AccessReadSubjectSemantics.EXPERT")
            .contains("ExpertQuestionAnalyticsReadRepository")
            .contains("ExpertQuestionAnalyticsReadCriteria")
            .contains("@Service")
            .contains("@Transactional(readOnly = true)")
            .doesNotContain("@ConditionalOnBean")
            .doesNotContain("org.springframework.boot.autoconfigure.condition")
            .doesNotContain("com.vladislav.training.platform.analytics.service.AnalyticsQueryService")
            .doesNotContain("import com.vladislav.training.platform.analytics.service.AnalyticsQueryService")
            .doesNotContain("AnalyticsQueryService analyticsQueryService")
            .doesNotContain("AnalyticsQueryService.class")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagerialReadScope")
            .doesNotContain("ManagerialReadScopeProjectionService")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("ManagerialVisibleUsersRestrictionBuilder")
            .doesNotContain("UserOrgReadScopeJpaSupport")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("ContentLifecycleController")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("ManagerialHistoricalAnalyticsQueryService")
            .doesNotContain("ManagerialCurrentSupervisionQueryService")
            .doesNotContain("EntityManager")
            .doesNotContain("JpaRepository")
            .doesNotContain("Controller")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(")
            .doesNotContain("publish(")
            .doesNotContain("archive(")
            .doesNotContain("assignActiveFinal")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");

        assertThat(fieldTypes(ExpertQuestionAnalyticsQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                ExpertQuestionAnalyticsReadRepository.class,
                AccessSpecificationPolicy.class
            )
            .doesNotContain(
                CapabilityAdmissionPolicy.class,
                QuestionCommandService.class,
                QuestionLifecycleService.class,
                ContentLifecycleController.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                com.vladislav.training.platform.analytics.query.ManagerialHistoricalAnalyticsQueryService.class,
                com.vladislav.training.platform.assignment.service.ManagerialCurrentSupervisionQueryService.class,
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                ResultRecordingService.class
            );
    }

    private ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery validQuery() {
        return new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
            101L,
            EFFECTIVE_AT,
            PERIOD_START,
            PERIOD_END
        );
    }

    private ExpertQuestionAnalyticsQueryServiceImpl service() {
        return new ExpertQuestionAnalyticsQueryServiceImpl(
            expertQuestionAnalyticsReadRepository,
            accessSpecificationPolicy
        );
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }
}
