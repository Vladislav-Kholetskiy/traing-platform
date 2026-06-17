package com.vladislav.training.platform.analytics.infrastructure.scheduler;

import com.vladislav.training.platform.analytics.service.AnalyticsCampaignAggregateSourceReader;
import com.vladislav.training.platform.analytics.service.AnalyticsRefreshService;
import com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildService;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик {@code AnalyticsRebuildScheduler}.
 */
@Component
final class AnalyticsRebuildScheduler {

    private final AnalyticsResultRebuildService analyticsResultRebuildService;
    private final AnalyticsRefreshService analyticsRefreshService;
    private final AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader;
    private final UtcClock utcClock;
    private final int resultRebuildLookbackDays;

    AnalyticsRebuildScheduler(
        AnalyticsResultRebuildService analyticsResultRebuildService,
        AnalyticsRefreshService analyticsRefreshService,
        AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader,
        UtcClock utcClock,
        @Value("${analytics.scheduler.result-rebuild-lookback-days:7}") int resultRebuildLookbackDays
    ) {
        this.analyticsResultRebuildService = Objects.requireNonNull(
            analyticsResultRebuildService,
            "analyticsResultRebuildService must not be null"
        );
        this.analyticsRefreshService = Objects.requireNonNull(
            analyticsRefreshService,
            "analyticsRefreshService must not be null"
        );
        this.campaignAggregateSourceReader = Objects.requireNonNull(
            campaignAggregateSourceReader,
            "campaignAggregateSourceReader must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
        if (resultRebuildLookbackDays <= 0) {
            throw new IllegalArgumentException("resultRebuildLookbackDays must be positive");
        }
        this.resultRebuildLookbackDays = resultRebuildLookbackDays;
    }

    @Scheduled(cron = "${analytics.scheduler.result-rebuild-cron:0 0 1 * * *}", zone = "UTC")
    void rebuildRecentResultAnalyticsBuckets() {
        LocalDate currentUtcDate = utcClock.now().atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate firstBucketDate = currentUtcDate.minusDays(resultRebuildLookbackDays - 1L);

        for (LocalDate bucketDate = firstBucketDate; !bucketDate.isAfter(currentUtcDate); bucketDate = bucketDate.plusDays(1)) {
            Instant periodStartInclusive = bucketDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant periodEndExclusive = bucketDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            analyticsResultRebuildService.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);
        }
    }

    @Scheduled(cron = "${analytics.scheduler.campaign-refresh-cron:0 30 1 * * *}", zone = "UTC")
    void refreshCampaignAggregateCandidates() {
        List<Long> campaignIds = campaignAggregateSourceReader.readAllCampaignIds();
        for (Long campaignId : campaignIds) {
            analyticsRefreshService.refreshCampaignAggregate(campaignId);
        }
    }
}

