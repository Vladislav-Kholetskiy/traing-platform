package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScopeProjectionService;
import com.vladislav.training.platform.assignment.repository.ManagerialCurrentSupervisionReadRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ManagerialCurrentSupervisionQueryServiceImpl implements ManagerialCurrentSupervisionQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private static final String DENIAL_MESSAGE =
        "Actor is not authorized to read managerial current supervision";

    private final ManagerialCurrentSupervisionReadRepository managerialCurrentSupervisionReadRepository;
    private final ManagerialReadScopeProjectionService managerialReadScopeProjectionService;

    ManagerialCurrentSupervisionQueryServiceImpl(
        ManagerialCurrentSupervisionReadRepository managerialCurrentSupervisionReadRepository,
        ManagerialReadScopeProjectionService managerialReadScopeProjectionService
    ) {
        this.managerialCurrentSupervisionReadRepository = Objects.requireNonNull(
            managerialCurrentSupervisionReadRepository,
            "managerialCurrentSupervisionReadRepository must not be null"
        );
        this.managerialReadScopeProjectionService = Objects.requireNonNull(
            managerialReadScopeProjectionService,
            "managerialReadScopeProjectionService must not be null"
        );
    }

    @Override
    public List<ManagerialCurrentSupervisionRow> findCurrentSupervision(ManagerialCurrentSupervisionQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        ManagerialReadScope managerialReadScope = managerialReadScopeProjectionService.project(
            query.actorUserId(),
            query.effectiveAt(),
            AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
        );
        ensureReadAllowed(managerialReadScope);

        return managerialCurrentSupervisionReadRepository.findCurrentSupervisionRows(
            new ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadCriteria(managerialReadScope)
        ).stream()
            .map(this::toQueryRow)
            .toList();
    }

    private ManagerialCurrentSupervisionRow toQueryRow(
        ManagerialCurrentSupervisionReadRepository.ManagerialCurrentSupervisionReadRow row
    ) {
        return new ManagerialCurrentSupervisionRow(
            row.assignmentId(),
            row.userId(),
            row.userDisplayName(),
            row.courseId(),
            row.courseName(),
            row.assignmentTestCount(),
            row.assignedAt(),
            row.deadlineAt(),
            row.assignmentStatus()
        );
    }

    private void ensureReadAllowed(ManagerialReadScope managerialReadScope) {
        if (managerialReadScope.contour() != AccessReadArea.MANAGERIAL_CURRENT_SUPERVISION
            || managerialReadScope.subjectSemantics() != AccessReadSubjectSemantics.MANAGER
            || !managerialReadScope.readScope().readAllowed()) {
            throw new PolicyViolationException(NOT_AUTHORIZED, DENIAL_MESSAGE);
        }
    }
}
