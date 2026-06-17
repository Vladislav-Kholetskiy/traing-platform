package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsCampaignAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsDepartmentTopicAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsQuestionAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsUserTopicAggregateRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyticsRefreshServiceImpl implements AnalyticsRefreshService {

    private static final String NOT_IMPLEMENTED_MESSAGE =
        "Точечное обновление этого типа аналитики пока не реализовано.";

    private final AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader;
    private final AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository;
    private final AnalyticsResultRebuildService analyticsResultRebuildService;
    private final AnalyticsUserTopicAggregateRepository analyticsUserTopicAggregateRepository;
    private final AnalyticsDepartmentTopicAggregateRepository analyticsDepartmentTopicAggregateRepository;
    private final AnalyticsQuestionAggregateRepository analyticsQuestionAggregateRepository;
    private final UtcClock utcClock;

    @Autowired
    public AnalyticsRefreshServiceImpl(
        AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader,
        AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository,
        AnalyticsResultRebuildService analyticsResultRebuildService,
        AnalyticsUserTopicAggregateRepository analyticsUserTopicAggregateRepository,
        AnalyticsDepartmentTopicAggregateRepository analyticsDepartmentTopicAggregateRepository,
        AnalyticsQuestionAggregateRepository analyticsQuestionAggregateRepository,
        UtcClock utcClock
    ) {
        this.campaignAggregateSourceReader = Objects.requireNonNull(
            campaignAggregateSourceReader,
            "campaignAggregateSourceReader must not be null"
        );
        this.analyticsCampaignAggregateRepository = Objects.requireNonNull(
            analyticsCampaignAggregateRepository,
            "analyticsCampaignAggregateRepository must not be null"
        );
        this.analyticsResultRebuildService = Objects.requireNonNull(
            analyticsResultRebuildService,
            "analyticsResultRebuildService must not be null"
        );
        this.analyticsUserTopicAggregateRepository = Objects.requireNonNull(
            analyticsUserTopicAggregateRepository,
            "analyticsUserTopicAggregateRepository must not be null"
        );
        this.analyticsDepartmentTopicAggregateRepository = Objects.requireNonNull(
            analyticsDepartmentTopicAggregateRepository,
            "analyticsDepartmentTopicAggregateRepository must not be null"
        );
        this.analyticsQuestionAggregateRepository = Objects.requireNonNull(
            analyticsQuestionAggregateRepository,
            "analyticsQuestionAggregateRepository must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    public AnalyticsRefreshServiceImpl(
        AnalyticsCampaignAggregateSourceReader campaignAggregateSourceReader,
        AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository,
        UtcClock utcClock
    ) {
        this.campaignAggregateSourceReader = Objects.requireNonNull(
            campaignAggregateSourceReader,
            "campaignAggregateSourceReader must not be null"
        );
        this.analyticsCampaignAggregateRepository = Objects.requireNonNull(
            analyticsCampaignAggregateRepository,
            "analyticsCampaignAggregateRepository must not be null"
        );
        this.analyticsResultRebuildService = null;
        this.analyticsUserTopicAggregateRepository = null;
        this.analyticsDepartmentTopicAggregateRepository = null;
        this.analyticsQuestionAggregateRepository = null;
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @Override
    public AnalyticsUserTopicAggregate refreshUserTopicAggregate(
        Long userId,
        Long topicId,
        Instant periodStart,
        Instant periodEnd
    ) {
        requireResultRefreshDependencies();
        analyticsResultRebuildService.rebuildResultAnalytics(periodStart, periodEnd);
        return analyticsUserTopicAggregateRepository.findUserTopicAggregate(userId, topicId, periodStart, periodEnd);
    }

    @Override
    public AnalyticsDepartmentTopicAggregate refreshDepartmentTopicAggregate(
        Long organizationalUnitIdSnapshot,
        Long topicId,
        Instant periodStart,
        Instant periodEnd
    ) {
        requireResultRefreshDependencies();
        analyticsResultRebuildService.rebuildResultAnalytics(periodStart, periodEnd);
        return analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregate(
            organizationalUnitIdSnapshot,
            topicId,
            periodStart,
            periodEnd
        );
    }

    @Override
    public AnalyticsQuestionAggregate refreshQuestionAggregate(Long questionId, Instant periodStart, Instant periodEnd) {
        requireResultRefreshDependencies();
        analyticsResultRebuildService.rebuildResultAnalytics(periodStart, periodEnd);
        return analyticsQuestionAggregateRepository.findQuestionAggregate(questionId, periodStart, periodEnd);
    }

    @Override
    public AnalyticsCampaignAggregate refreshCampaignAggregate(Long campaignId) {
        Objects.requireNonNull(campaignId, "campaignId must not be null");

        AnalyticsCampaignAggregateSourceFacts sourceFacts = campaignAggregateSourceReader.readCampaignAggregateSourceFacts(campaignId);
        AnalyticsCampaignAggregate existingAggregate = findExistingAggregateOrNull(campaignId);
        Instant now = utcClock.now();

        AnalyticsCampaignAggregate aggregate = new AnalyticsCampaignAggregate(
            existingAggregate == null ? null : existingAggregate.id(),
            sourceFacts.campaignId(),
            sourceFacts.recipientSnapshotCount(),
            sourceFacts.nonCancelledAssignmentsFromCampaignSnapshot(),
            sourceFacts.completedAssignments(),
            sourceFacts.overdueAssignments(),
            sourceFacts.nonCancelledActivePool(),
            sourceFacts.cancelledAssignments(),
            calculateCoveragePercent(
                sourceFacts.completedAssignments(),
                sourceFacts.nonCancelledAssignmentsFromCampaignSnapshot()
            ),
            calculateCoveragePercent(sourceFacts.overdueAssignments(), sourceFacts.nonCancelledActivePool()),
            now,
            now,
            null
        );

        return analyticsCampaignAggregateRepository.saveCampaignAggregate(aggregate);
    }

    private AnalyticsCampaignAggregate findExistingAggregateOrNull(Long campaignId) {
        try {
            return analyticsCampaignAggregateRepository.findCampaignAggregateByCampaignId(campaignId);
        } catch (NotFoundException ignored) {
            return null;
        }
    }

    private BigDecimal calculateCoveragePercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private void requireResultRefreshDependencies() {
        if (analyticsResultRebuildService == null
            || analyticsUserTopicAggregateRepository == null
            || analyticsDepartmentTopicAggregateRepository == null
            || analyticsQuestionAggregateRepository == null) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
        }
    }
}
