package com.vladislav.training.platform.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadSubjectScope;
import com.vladislav.training.platform.access.service.AccessReadSubjectSemantics;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-08T10:00:00Z");

    @Mock
    private AssignmentReadRepository assignmentReadRepository;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver contextResolver;

    @Test
    void assignmentDetailReadUsesAssignmentContourThroughCanonicalPipeline() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        Assignment assignment = assignment(77L, 900L, 101L, 301L, AssignmentStatus.ASSIGNED);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentById(77L)).thenReturn(assignment);

        assertThat(service.findAssignmentById(77L)).isEqualTo(assignment);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentById(77L);
    }

    @Test
    void assignmentRootListReadByCampaignUsesAssignmentContourThroughCanonicalPipeline() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentListContext();
        Assignment assignment = assignment(77L, 900L, 101L, 301L, AssignmentStatus.ASSIGNED);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentsByCampaignId(900L)).thenReturn(List.of(assignment));

        assertThat(service.findAssignmentsByCampaignId(900L)).containsExactly(assignment);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentsByCampaignId(900L);
    }

    @Test
    void assignmentRootListReadByUserReturnsPersistedAssignmentsAndAllowsEmptyResult() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentListContext();
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentsByUserId(101L)).thenReturn(List.of());

        assertThat(service.findAssignmentsByUserId(101L)).isEmpty();

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentsByUserId(101L);
    }

    @Test
    void assignmentRootListReadByUserAndStatusUsesPersistedAssignmentRootFacts() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentListContext();
        Assignment assignment = assignment(77L, 900L, 101L, 301L, AssignmentStatus.OVERDUE);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentsByUserIdAndStatus(101L, AssignmentStatus.OVERDUE))
            .thenReturn(List.of(assignment));

        assertThat(service.findAssignmentsByUserIdAndStatus(101L, AssignmentStatus.OVERDUE)).containsExactly(assignment);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentsByUserIdAndStatus(101L, AssignmentStatus.OVERDUE);
    }

    @Test
    void activeAssignmentReadByUserAndCourseUsesAssignmentContourAndReturnsCurrentActiveRoot() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        Assignment assignment = assignment(88L, 900L, 101L, 301L, AssignmentStatus.ASSIGNED);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).thenReturn(assignment);

        assertThat(service.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).isEqualTo(assignment);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findActiveAssignmentByUserIdAndCourseId(101L, 301L);
    }

    @Test
    void assignmentTestDetailReadUsesPersistedSubordinateContour() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        AssignmentTest assignmentTest = assignmentTest(700L, 77L, 501L, 9001L);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentTestById(700L)).thenReturn(assignmentTest);

        assertThat(service.findAssignmentTestById(700L)).isEqualTo(assignmentTest);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentTestById(700L);
    }

    @Test
    void assignmentTestsByAssignmentIdUsePersistedSubordinateContourAndAllowEmptyResult() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentListContext();
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentTestsByAssignmentId(77L)).thenReturn(List.of());

        assertThat(service.findAssignmentTestsByAssignmentId(77L)).isEmpty();

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentTestsByAssignmentId(77L);
    }

    @Test
    void assignmentTestByCountedResultIdUsesPersistedAssignmentBoundLinkage() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        AssignmentTest assignmentTest = assignmentTest(701L, 77L, 501L, 9002L);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentTestByCountedResultId(9002L)).thenReturn(assignmentTest);

        assertThat(service.findAssignmentTestByCountedResultId(9002L)).isEqualTo(assignmentTest);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentTestByCountedResultId(9002L);
    }

    @Test
    void assignmentAdministrativeActionDetailReadReturnsTypedAdministrativeHistory() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        AssignmentAdministrativeAction administrativeAction = administrativeAction(501L, 77L);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentAdministrativeActionById(501L)).thenReturn(administrativeAction);

        assertThat(service.findAssignmentAdministrativeActionById(501L)).isEqualTo(administrativeAction);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentAdministrativeActionById(501L);
    }

    @Test
    void assignmentAdministrativeActionsByAssignmentIdUseTypedHistoryContour() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentListContext();
        AssignmentAdministrativeAction administrativeAction = administrativeAction(501L, 77L);
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentAdministrativeActionsByAssignmentId(77L))
            .thenReturn(List.of(administrativeAction));

        assertThat(service.findAssignmentAdministrativeActionsByAssignmentId(77L)).containsExactly(administrativeAction);

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.LIST, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentAdministrativeActionsByAssignmentId(77L);
    }

    @Test
    void subordinateNotFoundPropagatesFromPersistedRepositoryContour() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(true);
        when(assignmentReadRepository.findAssignmentAdministrativeActionById(999L))
            .thenThrow(new NotFoundException("Assignment administrative action not found: 999"));

        assertThatThrownBy(() -> service.findAssignmentAdministrativeActionById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verify(assignmentReadRepository).findAssignmentAdministrativeActionById(999L);
    }

    @Test
    void subordinateReadRejectsDeniedAssignmentContourThroughExistingAccessPipeline() {
        AssignmentQueryServiceImpl service = service();
        AccessPolicyQueryContext context = assignmentDetailContext();
        when(contextResolver.resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.canRead(context)).thenReturn(false);

        assertThatThrownBy(() -> service.findAssignmentTestById(700L))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("not authorized to read assignment detail data");

        verify(contextResolver).resolve(AccessReadArea.ASSIGNMENT, AccessReadType.DETAIL, "assignment");
        verify(accessSpecificationPolicy).canRead(context);
        verifyNoInteractions(assignmentReadRepository);
    }

    private AssignmentQueryServiceImpl service() {
        return new AssignmentQueryServiceImpl(
            assignmentReadRepository,
            accessSpecificationPolicy,
            contextResolver
        );
    }

    private AccessPolicyQueryContext assignmentDetailContext() {
        return new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private AccessPolicyQueryContext assignmentListContext() {
        return new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ASSIGNMENT,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "assignment",
            AccessReadSubjectScope.ACTOR_SELF,
            AccessReadSubjectSemantics.SELF
        );
    }

    private Assignment assignment(Long assignmentId, Long campaignId, Long userId, Long courseId, AssignmentStatus status) {
        return new Assignment(
            assignmentId,
            campaignId,
            userId,
            courseId,
            status,
            FIXED_INSTANT,
            FIXED_INSTANT.plusSeconds(86400),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentTest assignmentTest(Long assignmentTestId, Long assignmentId, Long testId, Long countedResultId) {
        return new AssignmentTest(
            assignmentTestId,
            assignmentId,
            testId,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            countedResultId,
            null,
            false,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentAdministrativeAction administrativeAction(Long actionId, Long assignmentId) {
        return new AssignmentAdministrativeAction(
            actionId,
            assignmentId,
            AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT,
            FIXED_INSTANT,
            "admin note",
            FIXED_INSTANT
        );
    }
}
