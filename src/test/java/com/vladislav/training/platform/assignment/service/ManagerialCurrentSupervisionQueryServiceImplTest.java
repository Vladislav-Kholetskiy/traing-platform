package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScopeProjectionService;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsQueryService;
import com.vladislav.training.platform.analytics.query.ManagerialHistoricalAnalyticsQueryService;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.service.AnalyticsRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import com.vladislav.training.platform.testing.service.AssignedAttemptEntryService;
import com.vladislav.training.platform.testing.service.AssignedAttemptSubmissionService;
import com.vladislav.training.platform.testing.service.SelfAttemptAbandonTerminalService;
import com.vladislav.training.platform.testing.service.SelfAttemptEntryService;
import java.lang.reflect.Field;
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
/**
 * Проверяет поведение {@code ManagerialCurrentSupervisionQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ManagerialCurrentSupervisionQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-27T20:00:00Z");

    @Mock
    private ManagerialCurrentSupervisionReadRepository managerialCurrentSupervisionReadRepository;
    @Mock
    private ManagerialReadScopeProjectionService managerialReadScopeProjectionService;

    @Test
    void allowPathProjectsManagerialScopeBeforeRepositoryReadAndMapsRows() {
        ManagerialCurrentSupervisionQueryServiceImpl service = service();
        ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery query =
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT);
        ManagerialReadScope managerialReadScope = managerialScope(
            101L,
            AccessReadScope.scoped(Set.of(30L), Set.of("/company/division-30"))
        );
        ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow repositoryRow =
            new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow(
                77L,
                201L,
                "Ivanov Ivan Ivanovich",
                501L,
                "Labor Safety",
                701L,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.plusSeconds(7200),
                AssignmentStatus.ASSIGNED
            );
        when(managerialReadScopeProjectionService.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        )).thenReturn(managerialReadScope);
        when(managerialCurrentSupervisionReadRepository.findCurrentSupervisionRows(anyCriteria()))
            .thenReturn(List.of(repositoryRow));

        List<ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow> rows =
            service.findCurrentSupervision(query);

        assertThat(rows).containsExactly(
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionRow(
                77L,
                201L,
                "Ivanov Ivan Ivanovich",
                501L,
                "Labor Safety",
                701L,
                FIXED_INSTANT.minusSeconds(3600),
                FIXED_INSTANT.plusSeconds(7200),
                AssignmentStatus.ASSIGNED
            )
        );

        var ordered = inOrder(managerialReadScopeProjectionService, managerialCurrentSupervisionReadRepository);
        ArgumentCaptor<ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria.class);
        ordered.verify(managerialReadScopeProjectionService).project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );
        ordered.verify(managerialCurrentSupervisionReadRepository).findCurrentSupervisionRows(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().managerialReadScope()).isEqualTo(managerialReadScope);
        verifyNoMoreInteractions(managerialReadScopeProjectionService, managerialCurrentSupervisionReadRepository);
    }

    @Test
    void denyAllProjectedScopeFailsClosedBeforeRepositoryMaterialization() {
        ManagerialCurrentSupervisionQueryServiceImpl service = service();
        ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery query =
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT);
        when(managerialReadScopeProjectionService.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        )).thenReturn(managerialScope(101L, AccessReadScope.denyAll()));

        assertThatThrownBy(() -> service.findCurrentSupervision(query))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("managerial current supervision");

        verify(managerialReadScopeProjectionService).project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );
        verifyNoInteractions(managerialCurrentSupervisionReadRepository);
        verifyNoMoreInteractions(managerialReadScopeProjectionService);
    }

    @Test
    void wrongProjectedContourFailsClosedBeforeRepositoryMaterialization() {
        ManagerialCurrentSupervisionQueryServiceImpl service = service();
        ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery query =
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT);
        when(managerialReadScopeProjectionService.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        )).thenReturn(new ManagerialReadScope(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.fullAccess()
        ));

        assertThatThrownBy(() -> service.findCurrentSupervision(query))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("managerial current supervision");

        verify(managerialReadScopeProjectionService).project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );
        verifyNoInteractions(managerialCurrentSupervisionReadRepository);
        verifyNoMoreInteractions(managerialReadScopeProjectionService);
    }

    @Test
    void emptyRepositoryResultStaysEmptyWithoutPostFiltering() {
        ManagerialCurrentSupervisionQueryServiceImpl service = service();
        ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery query =
            new ManagerialCurrentSupervisionQueryService.ManagerialCurrentSupervisionQuery(101L, FIXED_INSTANT);
        ManagerialReadScope managerialReadScope = managerialScope(101L, AccessReadScope.fullAccess());
        when(managerialReadScopeProjectionService.project(
            101L,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        )).thenReturn(managerialReadScope);
        when(managerialCurrentSupervisionReadRepository.findCurrentSupervisionRows(anyCriteria()))
            .thenReturn(List.of());

        assertThat(service.findCurrentSupervision(query)).isEmpty();

        verify(managerialCurrentSupervisionReadRepository).findCurrentSupervisionRows(
            new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(managerialReadScope)
        );
        verifyNoMoreInteractions(managerialReadScopeProjectionService, managerialCurrentSupervisionReadRepository);
    }

    @Test
    void implementationStaysSeparatedFromMutationAnalyticsHistoricalAndExpertDependencies() throws Exception {
        assertThat(fieldTypes(ManagerialCurrentSupervisionQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                ManagerialCurrentSupervisionReadRepository.class,
                ManagerialReadScopeProjectionService.class
            )
            .doesNotContain(
                AssignmentStatusRecalculationService.class,
                AnalyticsRefreshService.class,
                AnalyticsRebuildService.class,
                ResultRecordingService.class,
                AssignedAttemptEntryService.class,
                SelfAttemptEntryService.class,
                AssignedAttemptSubmissionService.class,
                SelfAttemptAbandonTerminalService.class,
                CapabilityAdmissionPolicy.class,
                ManagerialHistoricalAnalyticsQueryService.class,
                ManagerialHistoricalAnalyticsReadRepository.class,
                ExpertQuestionAnalyticsQueryService.class,
                ExpertQuestionAnalyticsReadRepository.class
            );

        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/assignment/service/ManagerialCurrentSupervisionQueryServiceImpl.java"
        ));
        assertThat(source)
            .contains("@Service")
            .contains("@Transactional(readOnly = true)")
            .contains("AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION")
            .doesNotContain("@ConditionalOnBean")
            .doesNotContain("org.springframework.boot.autoconfigure.condition")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagerialHistoricalAnalytics")
            .doesNotContain("ExpertQuestionAnalytics")
            .doesNotContain("analytics.query")
            .doesNotContain("analytics.repository")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("recalculate(")
            .doesNotContain("refresh(")
            .doesNotContain("rebuild(")
            .doesNotContain("Controller");
    }

    private ManagerialCurrentSupervisionQueryServiceImpl service() {
        return new ManagerialCurrentSupervisionQueryServiceImpl(
            managerialCurrentSupervisionReadRepository,
            managerialReadScopeProjectionService
        );
    }

    private ManagerialReadScope managerialScope(Long actorUserId, AccessReadScope readScope) {
        return new ManagerialReadScope(
            actorUserId,
            FIXED_INSTANT,
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION,
            readScope
        );
    }

    private ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria anyCriteria() {
        return org.mockito.ArgumentMatchers.any();
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .collect(Collectors.toUnmodifiableSet());
    }
}
