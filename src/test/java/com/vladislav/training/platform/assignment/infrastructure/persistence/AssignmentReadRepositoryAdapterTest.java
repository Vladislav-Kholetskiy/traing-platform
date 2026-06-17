package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentReadRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentSelfScopedReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentReadRepositoryAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentReadRepositoryAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-08T19:00:00Z");

    @Mock
    private SpringDataAssignmentCampaignJpaRepository assignmentCampaignJpaRepository;
    @Mock
    private SpringDataAssignmentCampaignCourseJpaRepository assignmentCampaignCourseJpaRepository;
    @Mock
    private SpringDataAssignmentCampaignRecipientSnapshotJpaRepository assignmentCampaignRecipientSnapshotJpaRepository;
    @Mock
    private SpringDataAssignmentJpaRepository assignmentJpaRepository;
    @Mock
    private SpringDataAssignmentTestJpaRepository assignmentTestJpaRepository;
    @Mock
    private SpringDataAssignmentAdministrativeActionJpaRepository assignmentAdministrativeActionJpaRepository;

    private final AssignmentPersistenceMapper mapper = new AssignmentPersistenceMapper();

    @Test
    void campaignReadRepositoryContractStaysNarrow() {
        assertThat(methodNames(AssignmentCampaignReadRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentCampaignById",
                "findAllAssignmentCampaigns",
                "findAssignmentCampaignsBySourceType",
                "findAssignmentCampaignCourseById",
                "findAssignmentCampaignCoursesByCampaignId",
                "findAssignmentCampaignRecipientSnapshotById",
                "findAssignmentCampaignRecipientSnapshotsByCampaignId",
                "findAssignmentCampaignRecipientSnapshotsByUserId"
            );
    }

    @Test
    void assignmentReadRepositoryContractStaysNarrow() {
        assertThat(methodNames(AssignmentReadRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentById",
                "findAllAssignments",
                "findAssignmentsByCampaignId",
                "findAssignmentsByUserId",
                "findAssignmentsByUserIdAndStatus",
                "findActiveAssignmentByUserIdAndCourseId",
                "findAssignmentTestById",
                "findAssignmentTestsByAssignmentId",
                "findAssignmentTestByCountedResultId",
                "findAssignmentAdministrativeActionById",
                "findAssignmentAdministrativeActionsByAssignmentId"
            );
    }

    @Test
    void assignmentSelfScopedReadRepositoryContractStaysActorBoundAndNarrow() {
        assertThat(methodNames(AssignmentSelfScopedReadRepository.class))
            .containsExactlyInAnyOrder(
                "findSelfScopedAssignments",
                "findSelfScopedAssignmentById",
                "findSelfScopedAssignmentTestsByAssignmentId"
            );
    }

    @Test
    void campaignReadAdapterMapsPostLaunchCampaignReads() {
        JpaAssignmentCampaignReadRepositoryAdapter adapter = new JpaAssignmentCampaignReadRepositoryAdapter(
            assignmentCampaignJpaRepository,
            assignmentCampaignCourseJpaRepository,
            assignmentCampaignRecipientSnapshotJpaRepository,
            mapper
        );
        AssignmentCampaignEntity campaignEntity = campaignEntity();
        AssignmentCampaignCourseEntity campaignCourseEntity = campaignCourseEntity(campaignEntity.getId(), 301L);
        AssignmentCampaignRecipientSnapshotEntity recipientSnapshotEntity =
            recipientSnapshotEntity(campaignEntity.getId(), 101L);

        when(assignmentCampaignJpaRepository.findAllByOrderByIdAsc()).thenReturn(List.of(campaignEntity));
        when(assignmentCampaignCourseJpaRepository.findAllByCampaignIdOrderByIdAsc(campaignEntity.getId()))
            .thenReturn(List.of(campaignCourseEntity));
        when(assignmentCampaignRecipientSnapshotJpaRepository.findAllByCampaignIdOrderByIdAsc(campaignEntity.getId()))
            .thenReturn(List.of(recipientSnapshotEntity));
        when(assignmentCampaignRecipientSnapshotJpaRepository.findAllByUserIdOrderByIdAsc(101L))
            .thenReturn(List.of(recipientSnapshotEntity));

        List<AssignmentCampaign> campaigns = adapter.findAllAssignmentCampaigns();
        List<AssignmentCampaignCourse> courses = adapter.findAssignmentCampaignCoursesByCampaignId(campaignEntity.getId());
        List<AssignmentCampaignRecipientSnapshot> snapshotsByCampaign =
            adapter.findAssignmentCampaignRecipientSnapshotsByCampaignId(campaignEntity.getId());
        List<AssignmentCampaignRecipientSnapshot> snapshotsByUser =
            adapter.findAssignmentCampaignRecipientSnapshotsByUserId(101L);

        assertThat(campaigns)
            .singleElement()
            .satisfies(campaign -> {
                assertThat(campaign.id()).isEqualTo(11L);
                assertThat(campaign.sourceType()).isEqualTo("ORG_UNIT");
            });
        assertThat(courses)
            .singleElement()
            .satisfies(course -> {
                assertThat(course.campaignId()).isEqualTo(11L);
                assertThat(course.courseId()).isEqualTo(301L);
            });
        assertThat(snapshotsByCampaign).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.campaignId()).isEqualTo(11L);
            assertThat(snapshot.userId()).isEqualTo(101L);
        });
        assertThat(snapshotsByUser).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.campaignId()).isEqualTo(11L);
            assertThat(snapshot.userId()).isEqualTo(101L);
        });
    }

    @Test
    void assignmentReadAdapterMapsNarrowAssignmentRootAndSubordinateReadsAndFailsClosedWhenMissingDetail() {
        JpaAssignmentReadRepositoryAdapter adapter = new JpaAssignmentReadRepositoryAdapter(
            assignmentJpaRepository,
            assignmentTestJpaRepository,
            assignmentAdministrativeActionJpaRepository,
            mapper
        );
        AssignmentEntity entity = assignmentEntity(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        AssignmentEntity secondEntity = assignmentEntity(32L, 11L, 101L, 302L, AssignmentStatus.OVERDUE);
        AssignmentTestEntity assignmentTestEntity = assignmentTestEntity(41L, 31L, 501L, 9001L);
        AssignmentAdministrativeActionEntity administrativeActionEntity = administrativeActionEntity(51L, 31L);

        when(assignmentJpaRepository.findById(31L)).thenReturn(Optional.of(entity));
        when(assignmentJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(assignmentJpaRepository.findAllByCampaignIdOrderByIdAsc(11L)).thenReturn(List.of(entity, secondEntity));
        when(assignmentJpaRepository.findAllByUserIdOrderByIdAsc(101L)).thenReturn(List.of(entity, secondEntity));
        when(assignmentJpaRepository.findAllByUserIdAndStatusOrderByIdAsc(101L, AssignmentStatus.OVERDUE))
            .thenReturn(List.of(secondEntity));
        when(assignmentJpaRepository.findFirstByUserIdAndCourseIdAndCancelledAtIsNullAndClosedAtIsNullOrderByIdDesc(101L, 301L))
            .thenReturn(Optional.of(entity));
        when(assignmentTestJpaRepository.findById(41L)).thenReturn(Optional.of(assignmentTestEntity));
        when(assignmentTestJpaRepository.findById(999L)).thenReturn(Optional.empty());
        when(assignmentTestJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L)).thenReturn(List.of(assignmentTestEntity));
        when(assignmentTestJpaRepository.findByCountedResultId(9001L)).thenReturn(Optional.of(assignmentTestEntity));
        when(assignmentAdministrativeActionJpaRepository.findById(51L)).thenReturn(Optional.of(administrativeActionEntity));
        when(assignmentAdministrativeActionJpaRepository.findById(998L)).thenReturn(Optional.empty());
        when(assignmentAdministrativeActionJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L))
            .thenReturn(List.of(administrativeActionEntity));

        Assignment assignment = adapter.findAssignmentById(31L);
        List<Assignment> assignmentsByCampaign = adapter.findAssignmentsByCampaignId(11L);
        List<Assignment> assignmentsByUser = adapter.findAssignmentsByUserId(101L);
        List<Assignment> assignmentsByUserAndStatus = adapter.findAssignmentsByUserIdAndStatus(101L, AssignmentStatus.OVERDUE);
        Assignment activeAssignment = adapter.findActiveAssignmentByUserIdAndCourseId(101L, 301L);
        AssignmentTest assignmentTest = adapter.findAssignmentTestById(41L);
        List<AssignmentTest> assignmentTests = adapter.findAssignmentTestsByAssignmentId(31L);
        AssignmentTest assignmentTestByCountedResult = adapter.findAssignmentTestByCountedResultId(9001L);
        AssignmentAdministrativeAction administrativeAction = adapter.findAssignmentAdministrativeActionById(51L);
        List<AssignmentAdministrativeAction> administrativeActions =
            adapter.findAssignmentAdministrativeActionsByAssignmentId(31L);

        assertThat(assignment.id()).isEqualTo(31L);
        assertThat(assignment.status()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(assignmentsByCampaign).extracting(Assignment::id).containsExactly(31L, 32L);
        assertThat(assignmentsByUser).extracting(Assignment::id).containsExactly(31L, 32L);
        assertThat(assignmentsByUserAndStatus).extracting(Assignment::id).containsExactly(32L);
        assertThat(activeAssignment.id()).isEqualTo(31L);
        assertThat(assignmentTest.id()).isEqualTo(41L);
        assertThat(assignmentTest.assignmentId()).isEqualTo(31L);
        assertThat(assignmentTests).extracting(AssignmentTest::id).containsExactly(41L);
        assertThat(assignmentTestByCountedResult.countedResultId()).isEqualTo(9001L);
        assertThat(administrativeAction.id()).isEqualTo(51L);
        assertThat(administrativeAction.assignmentId()).isEqualTo(31L);
        assertThat(administrativeActions).extracting(AssignmentAdministrativeAction::id).containsExactly(51L);
        assertThatThrownBy(() -> adapter.findAssignmentById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        assertThatThrownBy(() -> adapter.findAssignmentTestById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
        assertThatThrownBy(() -> adapter.findAssignmentAdministrativeActionById(998L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("998");
    }

    @Test
    void assignmentReadAdapterPreFiltersSelfScopedListAndDetailBeforeMaterialization() {
        JpaAssignmentReadRepositoryAdapter adapter = new JpaAssignmentReadRepositoryAdapter(
            assignmentJpaRepository,
            assignmentTestJpaRepository,
            assignmentAdministrativeActionJpaRepository,
            mapper
        );
        AssignmentEntity selfOwnedEntity = assignmentEntity(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        AssignmentEntity anotherSelfOwnedEntity = assignmentEntity(32L, 11L, 101L, 302L, AssignmentStatus.OVERDUE);
        Assignment selfOwnedAssignment = mapper.toDomain(selfOwnedEntity);
        Assignment anotherSelfOwnedAssignment = mapper.toDomain(anotherSelfOwnedEntity);

        when(assignmentJpaRepository.findAllByUserIdOrderByIdAsc(101L))
            .thenReturn(List.of(selfOwnedEntity, anotherSelfOwnedEntity));
        when(assignmentJpaRepository.findByIdAndUserId(31L, 101L))
            .thenReturn(Optional.of(selfOwnedEntity));
        when(assignmentJpaRepository.findByIdAndUserId(99L, 101L))
            .thenReturn(Optional.empty());

        assertThat(adapter.findSelfScopedAssignments(101L))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(selfOwnedAssignment, anotherSelfOwnedAssignment);
        assertThat(adapter.findSelfScopedAssignmentById(101L, 31L))
            .usingRecursiveComparison()
            .isEqualTo(selfOwnedAssignment);
        assertThatThrownBy(() -> adapter.findSelfScopedAssignmentById(101L, 99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("actorUserId=101")
            .hasMessageContaining("assignmentId=99");

        verify(assignmentJpaRepository).findAllByUserIdOrderByIdAsc(101L);
        verify(assignmentJpaRepository).findByIdAndUserId(31L, 101L);
        verify(assignmentJpaRepository).findByIdAndUserId(99L, 101L);
        verify(assignmentJpaRepository, never()).findById(31L);
        verify(assignmentJpaRepository, never()).findById(99L);
    }

    @Test
    void assignmentReadAdapterPreFiltersSelfScopedAssignmentTestsThroughOwnedAssignmentAnchor() {
        JpaAssignmentReadRepositoryAdapter adapter = new JpaAssignmentReadRepositoryAdapter(
            assignmentJpaRepository,
            assignmentTestJpaRepository,
            assignmentAdministrativeActionJpaRepository,
            mapper
        );
        AssignmentEntity selfOwnedEntity = assignmentEntity(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        AssignmentTestEntity firstTest = assignmentTestEntity(41L, 31L, 501L, null);
        AssignmentTestEntity secondTest = assignmentTestEntity(42L, 31L, 502L, 9002L);

        when(assignmentJpaRepository.findByIdAndUserId(31L, 101L))
            .thenReturn(Optional.of(selfOwnedEntity));
        when(assignmentJpaRepository.findByIdAndUserId(99L, 101L))
            .thenReturn(Optional.empty());
        when(assignmentTestJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L))
            .thenReturn(List.of(firstTest, secondTest));

        assertThat(adapter.findSelfScopedAssignmentTestsByAssignmentId(101L, 31L))
            .extracting(AssignmentTest::id)
            .containsExactly(41L, 42L);
        assertThatThrownBy(() -> adapter.findSelfScopedAssignmentTestsByAssignmentId(101L, 99L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("actorUserId=101")
            .hasMessageContaining("assignmentId=99");

        verify(assignmentJpaRepository).findByIdAndUserId(31L, 101L);
        verify(assignmentJpaRepository).findByIdAndUserId(99L, 101L);
        verify(assignmentTestJpaRepository).findAllByAssignmentIdOrderByIdAsc(31L);
        verify(assignmentTestJpaRepository, never()).findAllByAssignmentIdOrderByIdAsc(99L);
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
    }

    private AssignmentCampaignEntity campaignEntity() {
        AssignmentCampaignEntity entity = new AssignmentCampaignEntity();
        entity.setId(11L);
        entity.setName("Launched campaign read");
        entity.setDescription("read repository");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("ou-42");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignCourseEntity campaignCourseEntity(Long campaignId, Long courseId) {
        AssignmentCampaignCourseEntity entity = new AssignmentCampaignCourseEntity();
        entity.setId(21L);
        entity.setCampaignId(campaignId);
        entity.setCourseId(courseId);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentEntity assignmentEntity(
        Long assignmentId,
        Long campaignId,
        Long userId,
        Long courseId,
        AssignmentStatus status
    ) {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setId(assignmentId);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setStatus(status);
        entity.setAssignedAt(FIXED_INSTANT);
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(86400));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(
        Long assignmentTestId,
        Long assignmentId,
        Long testId,
        Long countedResultId
    ) {
        AssignmentTestEntity entity = new AssignmentTestEntity();
        entity.setId(assignmentTestId);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(countedResultId);
        entity.setClosed(false);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentAdministrativeActionEntity administrativeActionEntity(Long actionId, Long assignmentId) {
        AssignmentAdministrativeActionEntity entity = new AssignmentAdministrativeActionEntity();
        entity.setId(actionId);
        entity.setAssignmentId(assignmentId);
        entity.setActionType(AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT);
        entity.setOccurredAt(FIXED_INSTANT);
        entity.setNote("typed administrative action");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignRecipientSnapshotEntity recipientSnapshotEntity(Long campaignId, Long userId) {
        AssignmentCampaignRecipientSnapshotEntity entity = new AssignmentCampaignRecipientSnapshotEntity();
        entity.setId(51L);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setOrganizationalUnitIdSnapshot(301L);
        entity.setOrganizationalPathSnapshot("/company/division/unit");
        entity.setCapturedAt(FIXED_INSTANT);
        entity.setInclusionBasisCode("ORG_UNIT");
        entity.setEmployeeNumberSnapshot("EMP-001");
        entity.setFullNameSnapshot("Ivan Ivanov");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }
}
