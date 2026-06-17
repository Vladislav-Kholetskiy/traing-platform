package com.vladislav.training.platform.analytics.controller;

import com.vladislav.training.platform.analytics.controller.dto.AnalyticsResultRebuildRequest;
import com.vladislav.training.platform.analytics.controller.dto.AnalyticsResultRebuildResponse;
import com.vladislav.training.platform.analytics.service.AnalyticsAdminRebuildService;
import com.vladislav.training.platform.analytics.service.AnalyticsResultRebuildOutcome;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AnalyticsAdminRebuildController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/admin/analytics")
class AnalyticsAdminRebuildController {

    private final AnalyticsAdminRebuildService analyticsAdminRebuildService;
    private final InteractiveActorResolver interactiveActorResolver;

    AnalyticsAdminRebuildController(
        AnalyticsAdminRebuildService analyticsAdminRebuildService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.analyticsAdminRebuildService = Objects.requireNonNull(
            analyticsAdminRebuildService,
            "analyticsAdminRebuildService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @PostMapping("/result-rebuild")
    ResponseEntity<AnalyticsResultRebuildResponse> rebuildResultAnalytics(
        @Valid @RequestBody AnalyticsResultRebuildRequest request
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        AnalyticsResultRebuildOutcome outcome = analyticsAdminRebuildService.rebuildResultAnalytics(
            actorUserId,
            request.periodStart(),
            request.periodEnd()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AnalyticsResultRebuildResponse(
            outcome.periodStartInclusive(),
            outcome.periodEndExclusive(),
            outcome.sourceRowCount(),
            outcome.supportedTopicRowCount(),
            outcome.unsupportedTopicRowCount(),
            outcome.userTopicAggregateRowCount(),
            outcome.departmentTopicAggregateRowCount(),
            outcome.questionAggregateRowCount()
        ));
    }
}
