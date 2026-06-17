package com.vladislav.training.platform.analytics.controller;

import com.vladislav.training.platform.analytics.controller.dto.ExpertQuestionAnalyticsResponse;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsDto;
import com.vladislav.training.platform.analytics.query.ExpertQuestionAnalyticsQueryService;
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
 * Контроллер {@code ExpertQuestionAnalyticsController}.
 */
@RestController
@RequestMapping("/api/v1/expert/question-analytics")
class ExpertQuestionAnalyticsController {

    private final ExpertQuestionAnalyticsQueryService expertQuestionAnalyticsQueryService;
    private final InteractiveActorResolver interactiveActorResolver;
    private final UtcClock utcClock;

    ExpertQuestionAnalyticsController(
        ExpertQuestionAnalyticsQueryService expertQuestionAnalyticsQueryService,
        InteractiveActorResolver interactiveActorResolver,
        UtcClock utcClock
    ) {
        this.expertQuestionAnalyticsQueryService = Objects.requireNonNull(
            expertQuestionAnalyticsQueryService,
            "expertQuestionAnalyticsQueryService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
        this.utcClock = Objects.requireNonNull(utcClock, "utcClock must not be null");
    }

    @GetMapping
    List<ExpertQuestionAnalyticsResponse> findExpertQuestionAnalytics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd
    ) {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        List<ExpertQuestionAnalyticsDto> rows = expertQuestionAnalyticsQueryService.findQuestionAnalytics(
            new ExpertQuestionAnalyticsQueryService.ExpertQuestionAnalyticsQuery(
                actorUserId,
                utcClock.now(),
                periodStart,
                periodEnd
            )
        );
        return rows.stream().map(this::toResponse).toList();
    }

    private ExpertQuestionAnalyticsResponse toResponse(ExpertQuestionAnalyticsDto dto) {
        return new ExpertQuestionAnalyticsResponse(
            dto.questionId(),
            dto.periodStart(),
            dto.periodEnd(),
            dto.attemptCount(),
            dto.correctCount(),
            dto.incorrectCount(),
            dto.averageEarnedScore(),
            dto.calculatedAt(),
            dto.refreshedAt()
        );
    }
}
