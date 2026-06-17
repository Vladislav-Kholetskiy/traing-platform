package com.vladislav.training.platform.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsCampaignAggregateRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AnalyticsRefreshServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsRefreshServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-05-16T10:15:30Z");

    @Mock
    private AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader;
    @Mock
    private AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository;
    @Mock
    private UtcClock utcClock;
    @Captor
    private ArgumentCaptor<AnalyticsCampaignAggregate> aggregateCaptor;

    @Test
    void refreshCampaignAggregateCalculatesCountersAndUpsertsSingleAnalyticsRow() {
        when(utcClock.now()).thenReturn(NOW);
        when(campaignAggregateSourceReader.readCampaignAggregateSourceFacts(14L)).thenReturn(
            new AnalyticsCampaignAggregateSourceFacts(14L, 8, 6, 3, 2, 6, 2)
        );
        when(analyticsCampaignAggregateRepository.findCampaignAggregateByCampaignId(14L)).thenThrow(
            new NotFoundException("missing")
        );
        when(analyticsCampaignAggregateRepository.saveCampaignAggregate(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AnalyticsRefreshServiceImpl service = service();

        AnalyticsCampaignAggregate aggregate = service.refreshCampaignAggregate(14L);

        verify(analyticsCampaignAggregateRepository).saveCampaignAggregate(aggregateCaptor.capture());
        assertThat(aggregateCaptor.getValue()).isEqualTo(aggregate);
        assertThat(aggregate)
            .extracting(
                AnalyticsCampaignAggregate::id,
                AnalyticsCampaignAggregate::campaignId,
                AnalyticsCampaignAggregate::recipientSnapshotCount,
                AnalyticsCampaignAggregate::nonCancelledAssignmentsFromCampaignSnapshot,
                AnalyticsCampaignAggregate::completedAssignments,
                AnalyticsCampaignAggregate::overdueAssignments,
                AnalyticsCampaignAggregate::nonCancelledActivePool,
                AnalyticsCampaignAggregate::cancelledAssignments,
                AnalyticsCampaignAggregate::coveragePercent,
                AnalyticsCampaignAggregate::overduePercent,
                AnalyticsCampaignAggregate::calculatedAt,
                AnalyticsCampaignAggregate::refreshedAt,
                AnalyticsCampaignAggregate::reconciledAt
            )
            .containsExactly(
                null,
                14L,
                8,
                6,
                3,
                2,
                6,
                2,
                new BigDecimal("50.0000"),
                new BigDecimal("33.3333"),
                NOW,
                NOW,
                null
            );
    }

    @Test
    void refreshCampaignAggregateKeepsExistingAggregateIdentityAndNormalizesZeroDenominators() {
        when(utcClock.now()).thenReturn(NOW);
        when(campaignAggregateSourceReader.readCampaignAggregateSourceFacts(19L)).thenReturn(
            new AnalyticsCampaignAggregateSourceFacts(19L, 0, 0, 0, 0, 0, 0)
        );
        when(analyticsCampaignAggregateRepository.findCampaignAggregateByCampaignId(19L)).thenReturn(
            new AnalyticsCampaignAggregate(
                501L,
                19L,
                1,
                1,
                1,
                0,
                1,
                0,
                new BigDecimal("100.0000"),
                new BigDecimal("0.0000"),
                NOW.minusSeconds(60),
                NOW.minusSeconds(30),
                null
            )
        );
        when(analyticsCampaignAggregateRepository.saveCampaignAggregate(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AnalyticsCampaignAggregate aggregate = service().refreshCampaignAggregate(19L);

        assertThat(aggregate.id()).isEqualTo(501L);
        assertThat(aggregate.coveragePercent()).isEqualByComparingTo("0.0000");
        assertThat(aggregate.overduePercent()).isEqualByComparingTo("0.0000");
    }

    @Test
    void nonCampaignRefreshMethodsStayFailClosed() {
        AnalyticsRefreshServiceImpl service = service();

        assertThatThrownBy(() -> service.refreshUserTopicAggregate(1L, 2L, NOW, NOW.plusSeconds(1)))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> service.refreshDepartmentTopicAggregate(3L, 4L, NOW, NOW.plusSeconds(1)))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> service.refreshQuestionAggregate(5L, NOW, NOW.plusSeconds(1)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private AnalyticsRefreshServiceImpl service() {
        return new AnalyticsRefreshServiceImpl(
            campaignAggregateSourceReader,
            analyticsCampaignAggregateRepository,
            utcClock
        );
    }
}
