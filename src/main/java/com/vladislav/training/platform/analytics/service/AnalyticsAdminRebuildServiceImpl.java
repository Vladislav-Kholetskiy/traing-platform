package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyticsAdminRebuildServiceImpl implements AnalyticsAdminRebuildService {

    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final AnalyticsResultRebuildService analyticsResultRebuildService;

    public AnalyticsAdminRebuildServiceImpl(
        CapabilityAdmissionPolicy capabilityAdmissionPolicy,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        AnalyticsResultRebuildService analyticsResultRebuildService
    ) {
        this.capabilityAdmissionPolicy = Objects.requireNonNull(
            capabilityAdmissionPolicy,
            "capabilityAdmissionPolicy must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
        this.analyticsResultRebuildService = Objects.requireNonNull(
            analyticsResultRebuildService,
            "analyticsResultRebuildService must not be null"
        );
    }

    @Override
    public AnalyticsResultRebuildOutcome rebuildResultAnalytics(
        Long actorUserId,
        Instant periodStartInclusive,
        Instant periodEndExclusive
    ) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        capabilityAdmissionPolicy.check(capabilityAdmissionRequestFactory.createAnalyticsResultRebuild(actorUserId));
        return analyticsResultRebuildService.rebuildResultAnalytics(periodStartInclusive, periodEndExclusive);
    }
}
