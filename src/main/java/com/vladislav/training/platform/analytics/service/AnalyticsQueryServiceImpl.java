package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsDepartmentTopicAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsQuestionAggregate;
import com.vladislav.training.platform.analytics.domain.AnalyticsUserTopicAggregate;
import com.vladislav.training.platform.analytics.repository.AnalyticsCampaignAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsDepartmentTopicAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsQuestionAggregateRepository;
import com.vladislav.training.platform.analytics.repository.AnalyticsUserTopicAggregateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsUserTopicAggregateRepository analyticsUserTopicAggregateRepository;
    private final AnalyticsDepartmentTopicAggregateRepository analyticsDepartmentTopicAggregateRepository;
    private final AnalyticsQuestionAggregateRepository analyticsQuestionAggregateRepository;
    private final AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository;

    @Autowired
    AnalyticsQueryServiceImpl(
        AnalyticsUserTopicAggregateRepository analyticsUserTopicAggregateRepository,
        AnalyticsDepartmentTopicAggregateRepository analyticsDepartmentTopicAggregateRepository,
        AnalyticsQuestionAggregateRepository analyticsQuestionAggregateRepository,
        AnalyticsCampaignAggregateRepository analyticsCampaignAggregateRepository
    ) {
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
        this.analyticsCampaignAggregateRepository = Objects.requireNonNull(
            analyticsCampaignAggregateRepository,
            "analyticsCampaignAggregateRepository must not be null"
        );
    }

    AnalyticsQueryServiceImpl(
        AnalyticsUserTopicAggregateRepository analyticsUserTopicAggregateRepository,
        AnalyticsDepartmentTopicAggregateRepository analyticsDepartmentTopicAggregateRepository,
        AnalyticsQuestionAggregateRepository analyticsQuestionAggregateRepository
    ) {
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
        this.analyticsCampaignAggregateRepository = null;
    }

    @Override
    public AnalyticsUserTopicAggregate findUserTopicAggregateById(Long analyticsUserTopicAggregateId) {
        return analyticsUserTopicAggregateRepository.findUserTopicAggregateById(analyticsUserTopicAggregateId);
    }

    @Override
    public AnalyticsUserTopicAggregate findUserTopicAggregate(Long userId, Long topicId, Instant periodStart, Instant periodEnd) {
        return analyticsUserTopicAggregateRepository.findUserTopicAggregate(userId, topicId, periodStart, periodEnd);
    }

    @Override
    public List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByUserId(Long userId) {
        return analyticsUserTopicAggregateRepository.findUserTopicAggregatesByUserId(userId);
    }

    @Override
    public List<AnalyticsUserTopicAggregate> findUserTopicAggregatesByTopicId(Long topicId) {
        return analyticsUserTopicAggregateRepository.findUserTopicAggregatesByTopicId(topicId);
    }

    @Override
    public AnalyticsDepartmentTopicAggregate findDepartmentTopicAggregateById(Long analyticsDepartmentTopicAggregateId) {
        return analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregateById(analyticsDepartmentTopicAggregateId);
    }

    @Override
    public AnalyticsDepartmentTopicAggregate findDepartmentTopicAggregate(
        Long organizationalUnitIdSnapshot,
        Long topicId,
        Instant periodStart,
        Instant periodEnd
    ) {
        return analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregate(
            organizationalUnitIdSnapshot,
            topicId,
            periodStart,
            periodEnd
        );
    }

    @Override
    public List<AnalyticsDepartmentTopicAggregate> findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(
        Long organizationalUnitIdSnapshot
    ) {
        return analyticsDepartmentTopicAggregateRepository.findDepartmentTopicAggregatesByOrganizationalUnitIdSnapshot(
            organizationalUnitIdSnapshot
        );
    }

    @Override
    public AnalyticsQuestionAggregate findQuestionAggregateById(Long analyticsQuestionAggregateId) {
        return analyticsQuestionAggregateRepository.findQuestionAggregateById(analyticsQuestionAggregateId);
    }

    @Override
    public AnalyticsQuestionAggregate findQuestionAggregate(Long questionId, Instant periodStart, Instant periodEnd) {
        return analyticsQuestionAggregateRepository.findQuestionAggregate(questionId, periodStart, periodEnd);
    }

    @Override
    public List<AnalyticsQuestionAggregate> findQuestionAggregatesByQuestionId(Long questionId) {
        return analyticsQuestionAggregateRepository.findQuestionAggregatesByQuestionId(questionId);
    }

    @Override
    public AnalyticsCampaignAggregate findCampaignAggregateById(Long analyticsCampaignAggregateId) {
        return requireCampaignAggregateRepository().findCampaignAggregateById(analyticsCampaignAggregateId);
    }

    @Override
    public AnalyticsCampaignAggregate findCampaignAggregateByCampaignId(Long campaignId) {
        return requireCampaignAggregateRepository().findCampaignAggregateByCampaignId(campaignId);
    }

    private String campaignReadNotImplementedMessage() {
        return "Запрос агрегата по кампании пока не реализован; analytics contour read-only contour";
    }
    private AnalyticsCampaignAggregateRepository requireCampaignAggregateRepository() {
        if (analyticsCampaignAggregateRepository == null) {
            throw new UnsupportedOperationException(campaignReadNotImplementedMessage());
        }
        return analyticsCampaignAggregateRepository;
    }
}
