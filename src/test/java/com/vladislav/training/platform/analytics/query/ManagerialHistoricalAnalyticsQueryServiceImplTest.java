package com.vladislav.training.platform.analytics.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScopeProjectionService;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialDepartmentTopicAnalyticsReadRow;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialUserTopicAnalyticsReadRow;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository;
import com.vladislav.training.platform.assignment.service.AssignmentCommandService;
import com.vladislav.training.platform.assignment.service.AssignmentStatusRecalculationService;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.result.service.ResultRecordingService;
import java.io.IOException;
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
 * Проверяет поведение {@code ManagerialHistoricalAnalyticsQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ManagerialHistoricalAnalyticsQueryServiceImplTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-04-25T10:00:00Z");
    private static final Instant PERIOD_START = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-04-30T23:59:59Z");
    private static final Instant CALCULATED_AT = Instant.parse("2026-05-01T00:10:00Z");
    private static final Instant REFRESHED_AT = Instant.parse("2026-05-01T00:15:00Z");
    private static final Path IMPLEMENTATION_SOURCE = Path.of(
        "src/main/java/com/vladislav/training/platform/analytics/query/ManagerialHistoricalAnalyticsQueryServiceImpl.java"
    );

    @Mock
    private ManagerialHistoricalAnalyticsReadRepository managerialHistoricalAnalyticsReadRepository;
    @Mock
    private ManagerialReadScopeProjectionService managerialReadScopeProjectionService;

    @Test
    void serviceIsReadOnlyAndDependsOnlyOnRepositoryAndManagerialReadScopeProjectionService() {
        assertThat(ManagerialHistoricalAnalyticsQueryService.class.isAssignableFrom(
            ManagerialHistoricalAnalyticsQueryServiceImpl.class
        )).isTrue();
        assertThat(ManagerialHistoricalAnalyticsQueryServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ManagerialHistoricalAnalyticsQueryServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(ManagerialHistoricalAnalyticsQueryServiceImpl.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(fieldTypes(ManagerialHistoricalAnalyticsQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                ManagerialHistoricalAnalyticsReadRepository.class,
                ManagerialReadScopeProjectionService.class
            );
    }

    @Test
    void findUserTopicAnalyticsResolvesManagerialScopeWithHistoricalAnalyticsContourAndMapsRows() {
        ManagerialHistoricalAnalyticsQueryServiceImpl service = service();
        ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery query =
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(101L, EFFECTIVE_AT, PERIOD_START, PERIOD_END);
        ManagerialReadScope managerialReadScope = new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.fullAccess()
        );
        ManagerialUserTopicAnalyticsReadRow row = new ManagerialUserTopicAnalyticsReadRow(
            101L,
            "EMP-101",
            "РРІР°РЅРѕРІ",
            "РРІР°РЅ",
            "РРІР°РЅРѕРІРёС‡",
            501L,
            "РћС…СЂР°РЅР° С‚СЂСѓРґР°",
            PERIOD_START,
            PERIOD_END,
            new BigDecimal("84.2500"),
            new BigDecimal("91.5000"),
            12,
            3,
            CALCULATED_AT,
            REFRESHED_AT
        );

        when(managerialReadScopeProjectionService.project(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        )).thenReturn(managerialReadScope);
        when(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(any())).thenReturn(List.of(row));

        List<ManagerialUserTopicAnalyticsDto> result = service.findUserTopicAnalytics(query);

        assertThat(result).containsExactly(new ManagerialUserTopicAnalyticsDto(
            101L,
            "EMP-101",
            "РРІР°РЅРѕРІ РРІР°РЅ РРІР°РЅРѕРІРёС‡",
            501L,
            "РћС…СЂР°РЅР° С‚СЂСѓРґР°",
            PERIOD_START,
            PERIOD_END,
            new BigDecimal("84.2500"),
            new BigDecimal("91.5000"),
            12,
            3,
            CALCULATED_AT,
            REFRESHED_AT
        ));

        ArgumentCaptor<ManagerialHistoricalAnalyticsReadCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(ManagerialHistoricalAnalyticsReadCriteria.class);
        var ordered = inOrder(managerialReadScopeProjectionService, managerialHistoricalAnalyticsReadRepository);
        ordered.verify(managerialReadScopeProjectionService).project(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        ordered.verify(managerialHistoricalAnalyticsReadRepository).findUserTopicRows(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().managerialReadScope()).isEqualTo(managerialReadScope);
        assertThat(criteriaCaptor.getValue().periodStart()).isEqualTo(PERIOD_START);
        assertThat(criteriaCaptor.getValue().periodEnd()).isEqualTo(PERIOD_END);
        verifyNoMoreInteractions(managerialReadScopeProjectionService, managerialHistoricalAnalyticsReadRepository);
    }

    @Test
    void denyAllProjectedScopeFailsClosedBeforeUserTopicRepositoryRead() {
        ManagerialHistoricalAnalyticsQueryServiceImpl service = service();
        ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery query =
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
                101L,
                EFFECTIVE_AT,
                PERIOD_START,
                PERIOD_END
            );
        when(managerialReadScopeProjectionService.project(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        )).thenReturn(ManagerialReadScope.denyAll(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        ));

        assertThatThrownBy(() -> service.findUserTopicAnalytics(query))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("managerial historical analytics");

        verify(managerialReadScopeProjectionService).project(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        verifyNoInteractions(managerialHistoricalAnalyticsReadRepository);
        verifyNoMoreInteractions(managerialReadScopeProjectionService);
    }

    @Test
    void findDepartmentTopicAnalyticsResolvesManagerialScopeWithHistoricalAnalyticsContourAndMapsRows() {
        ManagerialHistoricalAnalyticsQueryServiceImpl service = service();
        ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery query =
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(202L, EFFECTIVE_AT, PERIOD_START, PERIOD_END);
        ManagerialReadScope managerialReadScope = new ManagerialReadScope(
            202L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.scoped(Set.of(42L), Set.of("/company/division/department"))
        );
        ManagerialDepartmentTopicAnalyticsReadRow row = new ManagerialDepartmentTopicAnalyticsReadRow(
            42L,
            "РЈСЃС‚Р°РЅРѕРІРєР° 42",
            "/company/division/department",
            501L,
            "РџСЂРѕРјС‹С€Р»РµРЅРЅР°СЏ Р±РµР·РѕРїР°СЃРЅРѕСЃС‚СЊ",
            PERIOD_START,
            PERIOD_END,
            new BigDecimal("77.2500"),
            new BigDecimal("61.5000"),
            25,
            4,
            CALCULATED_AT,
            REFRESHED_AT
        );

        when(managerialReadScopeProjectionService.project(
            202L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        )).thenReturn(managerialReadScope);
        when(managerialHistoricalAnalyticsReadRepository.findDepartmentTopicRows(any())).thenReturn(List.of(row));

        List<ManagerialDepartmentTopicAnalyticsDto> result = service.findDepartmentTopicAnalytics(query);

        assertThat(result).containsExactly(new ManagerialDepartmentTopicAnalyticsDto(
            42L,
            "РЈСЃС‚Р°РЅРѕРІРєР° 42",
            "/company/division/department",
            501L,
            "РџСЂРѕРјС‹С€Р»РµРЅРЅР°СЏ Р±РµР·РѕРїР°СЃРЅРѕСЃС‚СЊ",
            PERIOD_START,
            PERIOD_END,
            new BigDecimal("77.2500"),
            new BigDecimal("61.5000"),
            25,
            4,
            CALCULATED_AT,
            REFRESHED_AT
        ));

        ArgumentCaptor<ManagerialHistoricalAnalyticsReadCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(ManagerialHistoricalAnalyticsReadCriteria.class);
        var ordered = inOrder(managerialReadScopeProjectionService, managerialHistoricalAnalyticsReadRepository);
        ordered.verify(managerialReadScopeProjectionService).project(
            202L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        ordered.verify(managerialHistoricalAnalyticsReadRepository).findDepartmentTopicRows(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().managerialReadScope()).isEqualTo(managerialReadScope);
        assertThat(criteriaCaptor.getValue().periodStart()).isEqualTo(PERIOD_START);
        assertThat(criteriaCaptor.getValue().periodEnd()).isEqualTo(PERIOD_END);
        verifyNoMoreInteractions(managerialReadScopeProjectionService, managerialHistoricalAnalyticsReadRepository);
    }

    @Test
    void denyAllProjectedScopeFailsClosedBeforeDepartmentTopicRepositoryRead() {
        ManagerialHistoricalAnalyticsQueryServiceImpl service = service();
        ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery query =
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
                202L,
                EFFECTIVE_AT,
                PERIOD_START,
                PERIOD_END
            );
        when(managerialReadScopeProjectionService.project(
            202L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        )).thenReturn(ManagerialReadScope.denyAll(
            202L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        ));

        assertThatThrownBy(() -> service.findDepartmentTopicAnalytics(query))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("managerial historical analytics");

        verify(managerialReadScopeProjectionService).project(
            202L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        verifyNoInteractions(managerialHistoricalAnalyticsReadRepository);
        verifyNoMoreInteractions(managerialReadScopeProjectionService);
    }

    @Test
    void emptyRepositoryUserTopicResultStaysEmptyWithoutRefreshOrRebuildFallback() {
        ManagerialHistoricalAnalyticsQueryServiceImpl service = service();
        ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery query =
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
                101L,
                EFFECTIVE_AT,
                PERIOD_START,
                PERIOD_END
            );
        ManagerialReadScope managerialReadScope = new ManagerialReadScope(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS,
            AccessReadScope.fullAccess()
        );

        when(managerialReadScopeProjectionService.project(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        )).thenReturn(managerialReadScope);
        when(managerialHistoricalAnalyticsReadRepository.findUserTopicRows(any())).thenReturn(List.of());

        assertThat(service.findUserTopicAnalytics(query)).isEmpty();

        var ordered = inOrder(managerialReadScopeProjectionService, managerialHistoricalAnalyticsReadRepository);
        ArgumentCaptor<ManagerialHistoricalAnalyticsReadCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(ManagerialHistoricalAnalyticsReadCriteria.class);
        ordered.verify(managerialReadScopeProjectionService).project(
            101L,
            EFFECTIVE_AT,
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        ordered.verify(managerialHistoricalAnalyticsReadRepository).findUserTopicRows(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().managerialReadScope()).isEqualTo(managerialReadScope);
        assertThat(criteriaCaptor.getValue().periodStart()).isEqualTo(PERIOD_START);
        assertThat(criteriaCaptor.getValue().periodEnd()).isEqualTo(PERIOD_END);
        verifyNoMoreInteractions(managerialReadScopeProjectionService, managerialHistoricalAnalyticsReadRepository);
    }

    @Test
    void sourceLevelAntiDriftGuardRejectsGenericAnalyticsAndMutationDependencies() throws IOException {
        String source = Files.readString(IMPLEMENTATION_SOURCE);

        assertThat(source)
            .contains("ManagerialReadScopeProjectionService")
            .contains("ManagerialHistoricalAnalyticsReadRepository")
            .contains("AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS")
            .doesNotContain("com.vladislav.training.platform.analytics.service.AnalyticsQueryService")
            .doesNotContain("import com.vladislav.training.platform.analytics.service.AnalyticsQueryService")
            .doesNotContain("AnalyticsQueryService analyticsQueryService")
            .doesNotContain("AnalyticsQueryService.class")
            .doesNotContain("AccessSpecificationPolicy")
            .doesNotContain("CapabilityAdmissionPolicy")
            .doesNotContain("ManagementRelationRepository")
            .doesNotContain("ManagerialVisibleUsersRestrictionBuilder")
            .doesNotContain("UserOrgReadScopeJpaSupport")
            .doesNotContain("ManagerialCurrentSupervisionReadRepository")
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("AnalyticsCampaignAggregateRepository")
            .doesNotContain("AnalyticsCampaignAggregate")
            .doesNotContain("analytics_campaign_aggregate")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("EntityManager")
            .doesNotContain("Controller")
            .doesNotContain("save(")
            .doesNotContain("delete(")
            .doesNotContain("flush(")
            .doesNotContain("rebuild")
            .doesNotContain("refresh(")
            .doesNotContain("recalculate")
            .doesNotContain("recordResult");

        assertThat(fieldTypes(ManagerialHistoricalAnalyticsQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                ManagerialHistoricalAnalyticsReadRepository.class,
                ManagerialReadScopeProjectionService.class
            )
            .doesNotContain(
                ManagerialCurrentSupervisionReadRepository.class,
                AssignmentCommandService.class,
                AssignmentStatusRecalculationService.class,
                ResultRecordingService.class
            );
    }

    private ManagerialHistoricalAnalyticsQueryServiceImpl service() {
        return new ManagerialHistoricalAnalyticsQueryServiceImpl(
            managerialHistoricalAnalyticsReadRepository,
            managerialReadScopeProjectionService
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
