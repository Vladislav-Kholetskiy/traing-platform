package com.vladislav.training.platform.result.query;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.result.query.internal.SelfHistoricalResultReader;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Реализация сервиса чтения {@code SelfHistoricalResultQueryServiceImpl}.
 */

@Service
@Transactional(readOnly = true)
class SelfHistoricalResultQueryServiceImpl implements SelfHistoricalResultQueryService {

    private final SelfHistoricalResultReader SelfHistoricalResultReader;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    public SelfHistoricalResultQueryServiceImpl(
        SelfHistoricalResultReader SelfHistoricalResultReader,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.SelfHistoricalResultReader = Objects.requireNonNull(
            SelfHistoricalResultReader,
            "SelfHistoricalResultReader must not be null"
        );
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
        this.contextResolver = Objects.requireNonNull(contextResolver, "contextResolver must not be null");
    }

    @Override
    public List<SelfHistoricalResultReadModel> findSelfHistoricalResults(SelfHistoricalResultQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        AccessPolicyQueryContext context = contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_RESULT_HISTORY,
            AccessReadType.LIST,
            "self_result_history"
        );
        requireResolvedContext(context);
        requireMatchingActor(query, context);
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied"
            );
        }
        return SelfHistoricalResultReader.findSelfHistoricalResultRows(
            new SelfHistoricalResultReader.SelfHistoricalResultReadCriteria(query.actorUserId())
        ).stream()
            .map(this::toReadModel)
            .toList();
    }

    private void requireResolvedContext(AccessPolicyQueryContext context) {
        if (context == null) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied because policy context is unavailable"
            );
        }
    }

    private void requireMatchingActor(SelfHistoricalResultQuery query, AccessPolicyQueryContext context) {
        if (!Objects.equals(query.actorUserId(), context.actorUserId())) {
            throw new PolicyViolationException(
                "SELF_RESULT_HISTORY_DENIED",
                "Read access to self result history is denied because actor context does not match"
            );
        }
    }

    private SelfHistoricalResultReadModel toReadModel(SelfHistoricalResultReader.SelfHistoricalResultReadRow row) {
        return new SelfHistoricalResultReadModel(
            row.resultId(),
            row.recordedAt(),
            row.testAttemptId(),
            row.testId(),
            row.testName(),
            row.scorePercent(),
            row.score(),
            row.passed(),
            row.attemptMode(),
            row.assignmentId()
        );
    }
}

