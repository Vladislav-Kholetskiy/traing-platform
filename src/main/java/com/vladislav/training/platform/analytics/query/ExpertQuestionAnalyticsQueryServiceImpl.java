package com.vladislav.training.platform.analytics.query;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ExpertQuestionAnalyticsReadRepository.ExpertQuestionAnalyticsReadCriteria;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpertQuestionAnalyticsQueryServiceImpl implements ExpertQuestionAnalyticsQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String DENIAL_MESSAGE =
        "Actor is not authorized to read expert question analytics";

    private final ExpertQuestionAnalyticsReadRepository expertQuestionAnalyticsReadRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;

    public ExpertQuestionAnalyticsQueryServiceImpl(
        ExpertQuestionAnalyticsReadRepository expertQuestionAnalyticsReadRepository,
        AccessSpecificationPolicy accessSpecificationPolicy
    ) {
        this.expertQuestionAnalyticsReadRepository = Objects.requireNonNull(
            expertQuestionAnalyticsReadRepository,
            "expertQuestionAnalyticsReadRepository must not be null"
        );
        this.accessSpecificationPolicy = Objects.requireNonNull(
            accessSpecificationPolicy,
            "accessSpecificationPolicy must not be null"
        );
    }

    @Override
    public List<ExpertQuestionAnalyticsDto> findQuestionAnalytics(ExpertQuestionAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            query.actorUserId(),
            AccessReadArea.EXPERT_QUESTION_ANALYTICS,
            AccessReadType.ANALYTICS,
            query.effectiveAt(),
            null,
            null,
            "expert_question_analytics",
            AccessReadSubjectScope.UNSPECIFIED,
            AccessReadSubjectSemantics.EXPERT
        );
        ensureCanonicalExpertContext(context);
        AccessReadScope accessReadScope = accessSpecificationPolicy.resolveReadScope(context);
        ensureReadAllowed(accessReadScope);
        ExpertQuestionAnalyticsReadCriteria criteria = new ExpertQuestionAnalyticsReadCriteria(
            accessReadScope,
            query.periodStart(),
            query.periodEnd()
        );

        return expertQuestionAnalyticsReadRepository.findQuestionAnalyticsRows(criteria).stream()
            .map(row -> new ExpertQuestionAnalyticsDto(
                row.questionId(),
                row.periodStart(),
                row.periodEnd(),
                row.attemptCount(),
                row.correctCount(),
                row.incorrectCount(),
                row.averageEarnedScore(),
                row.calculatedAt(),
                row.refreshedAt()
            ))
            .toList();
    }

    private void ensureCanonicalExpertContext(AccessPolicyQueryContext context) {
        if (context.contour() != AccessReadArea.EXPERT_QUESTION_ANALYTICS
            || context.readType() != AccessReadType.ANALYTICS
            || context.subjectScope() != AccessReadSubjectScope.UNSPECIFIED
            || context.subjectSemantics() != AccessReadSubjectSemantics.EXPERT
            || !Objects.equals(context.targetEntityFamily(), "expert_question_analytics")
            || context.targetUserId() != null
            || context.targetOrganizationalUnitId() != null) {
            throw new PolicyViolationException(NOT_AUTHORIZED, DENIAL_MESSAGE);
        }
    }

    private void ensureReadAllowed(AccessReadScope accessReadScope) {
        if (accessReadScope == null || !accessReadScope.readAllowed()) {
            throw new PolicyViolationException(NOT_AUTHORIZED, DENIAL_MESSAGE);
        }
    }
}
