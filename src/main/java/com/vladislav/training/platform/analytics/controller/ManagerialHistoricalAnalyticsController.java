package com.vladislav.training.platform.analytics.controller;

import com.vladislav.training.platform.analytics.query.ManagerialDepartmentTopicAnalyticsDto;
import com.vladislav.training.platform.analytics.query.ManagerialHistoricalAnalyticsQueryService;
import com.vladislav.training.platform.analytics.query.ManagerialUserTopicAnalyticsDto;
import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code ManagerialHistoricalAnalyticsController}.
 */
@RestController
@RequestMapping("/api/v1/managerial/historical-analytics")
class ManagerialHistoricalAnalyticsController {

    private final ManagerialHistoricalAnalyticsQueryService managerialHistoricalAnalyticsQueryService;
    private final InteractiveActorResolver interactiveActorResolver;
    private final UtcClock utcClock;

    ManagerialHistoricalAnalyticsController(
        ManagerialHistoricalAnalyticsQueryService managerialHistoricalAnalyticsQueryService,
        InteractiveActorResolver interactiveActorResolver,
        UtcClock utcClock
    ) {
        this.managerialHistoricalAnalyticsQueryService = Objects.requireNonNull(
            managerialHistoricalAnalyticsQueryService,
            "managerialHistoricalAnalyticsQueryService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @GetMapping("/user-topic")
    List<ManagerialUserTopicAnalyticsDto> findUserTopicAnalytics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        Instant effectiveAt = utcClock.now();
        return managerialHistoricalAnalyticsQueryService.findUserTopicAnalytics(
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
                actorUserId,
                effectiveAt,
                periodStart,
                periodEnd
            )
        );
    }

    @GetMapping("/department-topic")
    List<ManagerialDepartmentTopicAnalyticsDto> findDepartmentTopicAnalytics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        Instant effectiveAt = utcClock.now();
        return managerialHistoricalAnalyticsQueryService.findDepartmentTopicAnalytics(
            new ManagerialHistoricalAnalyticsQueryService.ManagerialHistoricalAnalyticsQuery(
                actorUserId,
                effectiveAt,
                periodStart,
                periodEnd
            )
        );
    }
}
