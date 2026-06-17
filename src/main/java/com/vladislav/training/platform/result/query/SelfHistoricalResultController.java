package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.application.actor.InteractiveActorResolver;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Контроллер {@code SelfHistoricalResultController}.
 */

@RestController
@RequestMapping("/api/v1/self/results/history")
class SelfHistoricalResultController {

    private final SelfHistoricalResultQueryService selfHistoricalResultQueryService;
    private final InteractiveActorResolver interactiveActorResolver;

    SelfHistoricalResultController(
        SelfHistoricalResultQueryService selfHistoricalResultQueryService,
        InteractiveActorResolver interactiveActorResolver
    ) {
        this.selfHistoricalResultQueryService = Objects.requireNonNull(
            selfHistoricalResultQueryService,
            "selfHistoricalResultQueryService must not be null"
        );
        this.interactiveActorResolver = Objects.requireNonNull(
            interactiveActorResolver,
            "interactiveActorResolver must not be null"
        );
    }

    @GetMapping
    List<SelfHistoricalResultSummaryDto> findSelfHistoricalResults() {
        Long actorUserId = interactiveActorResolver.resolveActorUserId();
        return selfHistoricalResultQueryService.findSelfHistoricalResults(
            new SelfHistoricalResultQueryService.SelfHistoricalResultQuery(actorUserId)
        ).stream()
            .map(this::toSummaryDto)
            .toList();
    }

    private SelfHistoricalResultSummaryDto toSummaryDto(
        SelfHistoricalResultQueryService.SelfHistoricalResultReadModel readModel
    ) {
        return new SelfHistoricalResultSummaryDto(
            readModel.resultId(),
            readModel.recordedAt(),
            readModel.testAttemptId(),
            readModel.testId(),
            readModel.testName(),
            readModel.scorePercent(),
            readModel.score(),
            readModel.passed(),
            readModel.attemptMode(),
            readModel.assignmentId()
        );
    }
}
