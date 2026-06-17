package com.vladislav.training.platform.analytics.infrastructure.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.analytics.service.AnalyticsCampaignAggregateSourceReader;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AnalyticsRebuildScheduler}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsRebuildSchedulerTest {

    @Mock
    private AnalyticsResultRebuildService analyticsResultRebuildService;
    @Mock
    private AnalyticsRefreshService analyticsRefreshService;
    @Mock
    private AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader;
    @Mock
    private UtcClock utcClock;

    @Test
    void resultSchedulerRebuildsDeterministicUtcDailyBucketsForConfiguredLookback() {
        when(utcClock.now()).thenReturn(Instant.parse("2026-05-16T10:15:30Z"));

        AnalyticsRebuildScheduler scheduler = scheduler(3);

        scheduler.rebuildRecentResultAnalyticsBuckets();

        verify(analyticsResultRebuildService).rebuildResultAnalytics(
            Instant.parse("2026-05-14T00:00:00Z"),
            Instant.parse("2026-05-15T00:00:00Z")
        );
        verify(analyticsResultRebuildService).rebuildResultAnalytics(
            Instant.parse("2026-05-15T00:00:00Z"),
            Instant.parse("2026-05-16T00:00:00Z")
        );
        verify(analyticsResultRebuildService).rebuildResultAnalytics(
            Instant.parse("2026-05-16T00:00:00Z"),
            Instant.parse("2026-05-17T00:00:00Z")
        );
        verifyNoMoreInteractions(analyticsResultRebuildService);
    }

    @Test
    void campaignSchedulerDelegatesOnlyToCampaignRefreshContract() {
        when(campaignAggregateSourceReader.readAllCampaignIds()).thenReturn(List.of(10L, 20L, 30L));

        AnalyticsRebuildScheduler scheduler = scheduler(2);

        scheduler.refreshCampaignAggregateCandidates();

        verify(campaignAggregateSourceReader).readAllCampaignIds();
        verify(analyticsRefreshService).refreshCampaignAggregate(10L);
        verify(analyticsRefreshService).refreshCampaignAggregate(20L);
        verify(analyticsRefreshService).refreshCampaignAggregate(30L);
        verifyNoMoreInteractions(analyticsRefreshService);
        verifyNoMoreInteractions(analyticsResultRebuildService);
    }

    private AnalyticsRebuildScheduler scheduler(int lookbackDays) {
        return new AnalyticsRebuildScheduler(
            analyticsResultRebuildService,
            analyticsRefreshService,
            campaignAggregateSourceReader,
            utcClock,
            lookbackDays
        );
    }
}


