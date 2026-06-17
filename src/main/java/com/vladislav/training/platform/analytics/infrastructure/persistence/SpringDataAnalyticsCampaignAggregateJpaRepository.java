package com.vladislav.training.platform.analytics.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAnalyticsCampaignAggregateJpaRepository
    extends JpaRepository<AnalyticsCampaignAggregateEntity, Long> {

    Optional<AnalyticsCampaignAggregateEntity> findByCampaignId(Long campaignId);
}
