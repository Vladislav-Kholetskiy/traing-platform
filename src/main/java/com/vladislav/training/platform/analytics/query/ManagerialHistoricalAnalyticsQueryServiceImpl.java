package com.vladislav.training.platform.analytics.query;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScopeProjectionService;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository;
import com.vladislav.training.platform.analytics.repository.ManagerialHistoricalAnalyticsReadRepository.ManagerialHistoricalAnalyticsReadCriteria;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ManagerialHistoricalAnalyticsQueryServiceImpl implements ManagerialHistoricalAnalyticsQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String DENIAL_MESSAGE =
        "Actor is not authorized to read managerial historical analytics";

    private final ManagerialHistoricalAnalyticsReadRepository managerialHistoricalAnalyticsReadRepository;
    private final ManagerialReadScopeProjectionService managerialReadScopeProjectionService;

    public ManagerialHistoricalAnalyticsQueryServiceImpl(
        ManagerialHistoricalAnalyticsReadRepository managerialHistoricalAnalyticsReadRepository,
        ManagerialReadScopeProjectionService managerialReadScopeProjectionService
    ) {
        this.managerialHistoricalAnalyticsReadRepository = Objects.requireNonNull(
            managerialHistoricalAnalyticsReadRepository,
            "managerialHistoricalAnalyticsReadRepository must not be null"
        );
        this.managerialReadScopeProjectionService = Objects.requireNonNull(
            managerialReadScopeProjectionService,
            "managerialReadScopeProjectionService must not be null"
        );
    }

    @Override
    public java.util.List<ManagerialUserTopicAnalyticsDto> findUserTopicAnalytics(ManagerialHistoricalAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        var managerialReadScope = managerialReadScopeProjectionService.project(
            query.actorUserId(),
            query.effectiveAt(),
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        ensureReadAllowed(managerialReadScope);
        var criteria = new ManagerialHistoricalAnalyticsReadCriteria(
            managerialReadScope,
            query.periodStart(),
            query.periodEnd()
        );

        return managerialHistoricalAnalyticsReadRepository.findUserTopicRows(criteria).stream()
            .map(row -> new ManagerialUserTopicAnalyticsDto(
                row.userId(),
                row.employeeNumber(),
                formatDisplayName(row.lastName(), row.firstName(), row.middleName(), row.employeeNumber(), row.userId()),
                row.topicId(),
                row.topicName(),
                row.periodStart(),
                row.periodEnd(),
                row.averageScorePercent(),
                row.passRatePercent(),
                row.attemptCount(),
                row.errorCount(),
                row.calculatedAt(),
                row.refreshedAt()
            ))
            .toList();
    }

    @Override
    public java.util.List<ManagerialDepartmentTopicAnalyticsDto> findDepartmentTopicAnalytics(
        ManagerialHistoricalAnalyticsQuery query
    ) {
        Objects.requireNonNull(query, "query must not be null");

        var managerialReadScope = managerialReadScopeProjectionService.project(
            query.actorUserId(),
            query.effectiveAt(),
            AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
        );
        ensureReadAllowed(managerialReadScope);
        var criteria = new ManagerialHistoricalAnalyticsReadCriteria(
            managerialReadScope,
            query.periodStart(),
            query.periodEnd()
        );

        return managerialHistoricalAnalyticsReadRepository.findDepartmentTopicRows(criteria).stream()
            .map(row -> new ManagerialDepartmentTopicAnalyticsDto(
                row.organizationalUnitIdSnapshot(),
                row.organizationalUnitName(),
                row.organizationalPathSnapshot(),
                row.topicId(),
                row.topicName(),
                row.periodStart(),
                row.periodEnd(),
                row.averageScorePercent(),
                row.passRatePercent(),
                row.attemptCount(),
                row.errorCount(),
                row.calculatedAt(),
                row.refreshedAt()
            ))
            .toList();
    }

    private String formatDisplayName(
        String lastName,
        String firstName,
        String middleName,
        String employeeNumber,
        Long userId
    ) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, lastName);
        appendPart(builder, firstName);
        appendPart(builder, middleName);
        if (builder.length() > 0) {
            return builder.toString();
        }
        if (employeeNumber != null && !employeeNumber.isBlank()) {
            return employeeNumber;
        }
        return "РџСЂРѕС„РёР»СЊ #" + userId;
    }

    private void appendPart(StringBuilder builder, String part) {
        if (part == null) {
            return;
        }
        String trimmed = part.trim();
        if (trimmed.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(trimmed);
    }

    private void ensureReadAllowed(ManagerialReadScope managerialReadScope) {
        if (managerialReadScope.contour() != AccessReadArea.MANAGERIAL_HISTORICAL_ANALYTICS
            || managerialReadScope.subjectSemantics() != AccessReadSubjectSemantics.MANAGER
            || !managerialReadScope.readScope().readAllowed()) {
            throw new PolicyViolationException(NOT_AUTHORIZED, DENIAL_MESSAGE);
        }
    }
}
