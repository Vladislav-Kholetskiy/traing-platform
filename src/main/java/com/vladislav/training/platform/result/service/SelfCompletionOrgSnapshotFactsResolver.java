package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/**
 * Разрешитель {@code SelfCompletionOrgSnapshotFactsResolver}.
 */
@Component
@Transactional(readOnly = true)
@ConditionalOnBean({TestAttemptRepository.class, UserOrganizationAssignmentService.class, OrganizationQueryService.class})
class SelfCompletionOrgSnapshotFactsResolver implements SelfCompletionOrgSnapshotFactsReader {

    private final TestAttemptRepository testAttemptRepository;
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    private final OrganizationQueryService organizationQueryService;

    SelfCompletionOrgSnapshotFactsResolver(
        TestAttemptRepository testAttemptRepository,
        UserOrganizationAssignmentService userOrganizationAssignmentService,
        OrganizationQueryService organizationQueryService
    ) {
        this.testAttemptRepository = Objects.requireNonNull(
            testAttemptRepository,
            "testAttemptRepository must not be null"
        );
        this.userOrganizationAssignmentService = Objects.requireNonNull(
            userOrganizationAssignmentService,
            "userOrganizationAssignmentService must not be null"
        );
        this.organizationQueryService = Objects.requireNonNull(
            organizationQueryService,
            "organizationQueryService must not be null"
        );
    }

    @Override
    public SelfCompletionOrgSnapshotFacts readSelfCompletionOrgSnapshotFacts(Long testAttemptId) {
        Objects.requireNonNull(testAttemptId, "testAttemptId must not be null");

        TestAttempt terminalizedAttempt = testAttemptRepository.findTestAttemptById(testAttemptId);
        Instant terminalInstant = terminalInstantOf(terminalizedAttempt);
        List<UserOrganizationAssignment> activeAssignments = userOrganizationAssignmentService
            .findActiveOrganizationAssignmentsByUserId(terminalizedAttempt.userId(), terminalInstant)
            .stream()
            .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
            .toList();

        if (activeAssignments.size() != 1) {
            throw new ConflictException(
                "Self result recording requires exactly one active PRIMARY org assignment: testAttemptId="
                    + testAttemptId
                    + ", userId="
                    + terminalizedAttempt.userId()
            );
        }

        OrganizationalUnit organizationalUnit = organizationQueryService.findOrganizationalUnitById(
            activeAssignments.getFirst().organizationalUnitId()
        );
        return new SelfCompletionOrgSnapshotFacts(organizationalUnit.id(), organizationalUnit.path());
    }

    private Instant terminalInstantOf(TestAttempt terminalizedAttempt) {
        return switch (terminalizedAttempt.status()) {
            case COMPLETED -> requireTerminalInstant(terminalizedAttempt.completedAt(), terminalizedAttempt.status());
            case EXPIRED -> requireTerminalInstant(terminalizedAttempt.expiredAt(), terminalizedAttempt.status());
            case ABANDONED -> requireTerminalInstant(terminalizedAttempt.abandonedAt(), terminalizedAttempt.status());
            case STARTED, IN_PROGRESS -> throw new ConflictException(
                "Self completion org snapshot facts require a terminalized attempt: " + terminalizedAttempt.id()
            );
        };
    }

    private Instant requireTerminalInstant(Instant terminalInstant, TestAttemptStatus status) {
        if (terminalInstant == null) {
            throw new ConflictException("Terminalized self attempt must expose terminal timestamp for status " + status);
        }
        return terminalInstant;
    }
}

