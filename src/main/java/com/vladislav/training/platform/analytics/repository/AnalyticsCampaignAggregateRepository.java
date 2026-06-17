package com.vladislav.training.platform.analytics.repository;

import com.vladislav.training.platform.analytics.domain.AnalyticsCampaignAggregate;
import java.util.List;

public interface AnalyticsCampaignAggregateRepository {

    AnalyticsCampaignAggregate findCampaignAggregateById(Long analyticsCampaignAggregateId);

    AnalyticsCampaignAggregate findCampaignAggregateByCampaignId(Long campaignId);

    List<AnalyticsCampaignAggregate> findAllCampaignAggregates();

    AnalyticsCampaignAggregate saveCampaignAggregate(AnalyticsCampaignAggregate analyticsCampaignAggregate);

    void deleteAllCampaignAggregates();
}
