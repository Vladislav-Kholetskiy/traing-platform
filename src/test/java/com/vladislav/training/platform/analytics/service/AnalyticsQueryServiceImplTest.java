package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsCampaignAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsDepartmentTopicAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsQuestionAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsUserTopicAggregateRepository;
import java.lang.reflect.Field;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет поведение {@code AnalyticsQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T19:00:00Z");

    @Mock
    private AnalyticsUserTopicAggregateRepository analyticsUserTopicAggregateRepository;
    @Mock
    private AnalyticsDepartmentTopicAggregateRepository analyticsDepartmentTopicAggregateRepository;
    @Mock
    private AnalyticsQuestionAggregateRepository analyticsQuestionAggregateRepository;
    @Mock
    private AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository;

    @Test
    void serviceStaysReadOnlyAndDependsOnlyOnAnalyticsReadRepositories() {
        assertThat(AnalyticsQueryService.class.isAssignableFrom(AnalyticsQueryServiceImpl.class)).isTrue();
        assertThat(AnalyticsQueryServiceImpl.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(AnalyticsQueryServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(AnalyticsQueryServiceImpl.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(fieldTypes(AnalyticsQueryServiceImpl.class))
            .containsExactlyInAnyOrder(
                AnalyticsUserTopicAggregateRepository.class,
                AnalyticsDepartmentTopicAggregateRepository.class,
                AnalyticsQuestionAggregateRepository.class,
                AnalyticsCampaignAggregateRepository.class
            );
    }

    @Test
    void serviceDelegatesUserDepartmentQuestionAndCampaignReadsToRepositoriesOnly() {
        AnalyticsQueryServiceImpl service = service();
        AnalyticsUserTopicAggregate userAggregate = userAggregate();
        AnalyticsDepartmentTopicAggregate departmentAggregate = departmentAggregate();
        AnalyticsQuestionAggregate questionAggregate = questionAggregate();
        com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate campaignAggregate = campaignAggregate();

        when(analyticsUserTopicAggregateRepository.findUserTopicAggregateById(11L)).thenReturn(userAggregate);
        when(analyticsUserTopicAggregateRepository.findUserTopicAggregate(101L, 501L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600)))
            .thenReturn(userAggregate);
        when(analyticsUserTopicAggregateRepository.findUserTopicAggregatesByUserId(101L)).thenReturn(List.of(userAggregate));
        when(analyticsUserTopicAggregateRepository.findUserTopicAggregatesByTopicId(501L)).thenReturn(List.of(userAggregate));

        when(analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregateById(21L)).thenReturn(departmentAggregate);
        when(analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregate(
            301L,
            501L,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(3600)
        )).thenReturn(departmentAggregate);
        when(analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(301L))
            .thenReturn(List.of(departmentAggregate));

        when(analyticsQuestionAggregateRepository.findQuestionAggregateById(31L)).thenReturn(questionAggregate);
        when(analyticsQuestionAggregateRepository.findQuestionAggregate(701L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600)))
            .thenReturn(questionAggregate);
        when(analyticsQuestionAggregateRepository.findQuestionAggregatesByQuestionId(701L)).thenReturn(List.of(questionAggregate));
        when(analyticsCampaignAggregateRepository.findCampaignAggregateById(41L)).thenReturn(campaignAggregate);
        when(analyticsCampaignAggregateRepository.findCampaignAggregateByCampaignId(42L)).thenReturn(campaignAggregate);

        assertThat(service.findUserTopicAggregateById(11L)).isEqualTo(userAggregate);
        assertThat(service.findUserTopicAggregate(101L, 501L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600)))
            .isEqualTo(userAggregate);
        assertThat(service.findUserTopicAggregatesByUserId(101L)).containsExactly(userAggregate);
        assertThat(service.findUserTopicAggregatesByTopicId(501L)).containsExactly(userAggregate);

        assertThat(service.findDepartmentTopicAggregateById(21L)).isEqualTo(departmentAggregate);
        assertThat(service.findDepartmentTopicAggregate(301L, 501L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600)))
            .isEqualTo(departmentAggregate);
        assertThat(service.findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(301L)).containsExactly(departmentAggregate);

        assertThat(service.findQuestionAggregateById(31L)).isEqualTo(questionAggregate);
        assertThat(service.findQuestionAggregate(701L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600)))
            .isEqualTo(questionAggregate);
        assertThat(service.findQuestionAggregatesByQuestionId(701L)).containsExactly(questionAggregate);
        assertThat(service.findCampaignAggregateById(41L)).isEqualTo(campaignAggregate);
        assertThat(service.findCampaignAggregateByCampaignId(42L)).isEqualTo(campaignAggregate);

        verify(analyticsUserTopicAggregateRepository).findUserTopicAggregateById(11L);
        verify(analyticsUserTopicAggregateRepository)
            .findUserTopicAggregate(101L, 501L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600));
        verify(analyticsUserTopicAggregateRepository).findUserTopicAggregatesByUserId(101L);
        verify(analyticsUserTopicAggregateRepository).findUserTopicAggregatesByTopicId(501L);

        verify(analyticsDepartmentTopicAggregateRepository).findDepartmentTopicAggregateById(21L);
        verify(analyticsDepartmentTopicAggregateRepository)
            .findDepartmentTopicAggregate(301L, 501L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600));
        verify(analyticsDepartmentTopicAggregateRepository).findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(301L);

        verify(analyticsQuestionAggregateRepository).findQuestionAggregateById(31L);
        verify(analyticsQuestionAggregateRepository)
            .findQuestionAggregate(701L, FIXED_INSTANT, FIXED_INSTANT.plusSeconds(3600));
        verify(analyticsQuestionAggregateRepository).findQuestionAggregatesByQuestionId(701L);
        verify(analyticsCampaignAggregateRepository).findCampaignAggregateById(41L);
        verify(analyticsCampaignAggregateRepository).findCampaignAggregateByCampaignId(42L);

        verifyNoMoreInteractions(
            analyticsUserTopicAggregateRepository,
            analyticsDepartmentTopicAggregateRepository,
            analyticsQuestionAggregateRepository,
            analyticsCampaignAggregateRepository
        );
    }

    @Test
    void campaignMethodsFailClosedUntilCampaignReadFoundationExists() {
        AnalyticsQueryServiceImpl service = new AnalyticsQueryServiceImpl(
            analyticsUserTopicAggregateRepository,
            analyticsDepartmentTopicAggregateRepository,
            analyticsQuestionAggregateRepository
        );

        assertThatThrownBy(() -> service.findCampaignAggregateById(41L))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("analytics contour read-only contour");
        assertThatThrownBy(() -> service.findCampaignAggregateByCampaignId(42L))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("analytics contour read-only contour");
    }

    @Test
    void sourceStaysFreeFromRefreshRebuildWriteCallsAndNonAnalyticsDependencies() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/analytics/service/AnalyticsQueryServiceImpl.java"
        ));

        assertThat(source)
            .doesNotContain("AnalyticsRefreshService")
            .doesNotContain("AnalyticsRebuildService")
            .doesNotContain("ResultRecordingService")
            .doesNotContain("AssignmentStatusRecalculationService")
            .doesNotContain("QuestionCommandService")
            .doesNotContain("QuestionLifecycleService")
            .doesNotContain("QuestionQueryService")
            .doesNotContain("controller.")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".flush(")
            .doesNotContain("rebuild")
            .doesNotContain("refresh");
    }

    private AnalyticsQueryServiceImpl service() {
        return new AnalyticsQueryServiceImpl(
            analyticsUserTopicAggregateRepository,
            analyticsDepartmentTopicAggregateRepository,
            analyticsQuestionAggregateRepository,
            analyticsCampaignAggregateRepository
        );
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields()).map(Field::getType).collect(Collectors.toUnmodifiableSet());
    }

    private AnalyticsUserTopicAggregate userAggregate() {
        return new AnalyticsUserTopicAggregate(
            11L,
            101L,
            501L,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(3600),
            901L,
            FIXED_INSTANT.plusSeconds(60),
            new java.math.BigDecimal("88.5000"),
            true,
            new java.math.BigDecimal("81.2500"),
            new java.math.BigDecimal("66.6700"),
            12,
            4,
            FIXED_INSTANT.plusSeconds(120),
            FIXED_INSTANT.plusSeconds(150),
            FIXED_INSTANT.plusSeconds(180)
        );
    }

    private AnalyticsDepartmentTopicAggregate departmentAggregate() {
        return new AnalyticsDepartmentTopicAggregate(
            21L,
            301L,
            "/company/division/unit",
            501L,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(3600),
            new java.math.BigDecimal("78.2500"),
            new java.math.BigDecimal("61.5000"),
            20,
            7,
            FIXED_INSTANT.plusSeconds(200),
            FIXED_INSTANT.plusSeconds(220),
            FIXED_INSTANT.plusSeconds(240)
        );
    }

    private AnalyticsQuestionAggregate questionAggregate() {
        return new AnalyticsQuestionAggregate(
            31L,
            701L,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(3600),
            15,
            8,
            3,
            new java.math.BigDecimal("4.7500"),
            FIXED_INSTANT.plusSeconds(260),
            FIXED_INSTANT.plusSeconds(280),
            FIXED_INSTANT.plusSeconds(300)
        );
    }

    private com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate campaignAggregate() {
        return new com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate(
            41L,
            42L,
            18,
            17,
            12,
            3,
            17,
            1,
            new java.math.BigDecimal("70.5882"),
            new java.math.BigDecimal("17.6471"),
            FIXED_INSTANT.plusSeconds(320),
            FIXED_INSTANT.plusSeconds(340),
            FIXED_INSTANT.plusSeconds(360)
        );
    }
}
