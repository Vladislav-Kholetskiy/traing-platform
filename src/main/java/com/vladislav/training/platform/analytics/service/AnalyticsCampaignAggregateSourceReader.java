package com.vladislav.training.platform.analytics.service;

import java.util.List;

/**
 * Читатель {@code AnalyticsCampaignAggregateSourceReader}.
 */
public interface AnalyticsCampaignAggregateSourceReader {

    AnalyticsCampaignAggregateSourceFacts readCampaignAggregateSourceFacts(Long campaignId);

    List<Long> readAllCampaignIds();
}
