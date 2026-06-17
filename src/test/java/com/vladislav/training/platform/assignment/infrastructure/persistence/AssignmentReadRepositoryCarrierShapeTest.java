package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
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
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет форму и состав {@code AssignmentReadRepositoryCarrier}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentReadRepositoryCarrierShapeTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-10T13:00:00Z");

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
    @Mock
    private AssignmentPersistenceMapper mapper;

    @Test
    void readRepositoryContractsStayNarrowAndSeparatedFromOwnerAndPreviewSemantics() {
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
            )
            .doesNotContain(
                "findPreviewRecipientPool",
                "saveAssignmentCampaign",
                "searchAssignmentCampaigns",
                "reportAssignmentCampaigns"
            );

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
            )
            .doesNotContain(
                "saveAssignment",
                "findPreviewRecipientPool",
                "reportAssignments",
                "findAuditEvents"
            );

        assertThat(methodNames(AssignmentSelfScopedReadRepository.class))
            .containsExactlyInAnyOrder(
                "findSelfScopedAssignments",
                "findSelfScopedAssignmentById",
                "findSelfScopedAssignmentTestsByAssignmentId"
            )
            .doesNotContain(
                "findAssignmentsByUserId",
                "findAssignmentById",
                "reportAssignments",
                "findAuditEvents"
            );
    }

    @Test
    void readAdaptersStayRepositoryAnnotatedTransactionalAndUseOnlyExpectedCarriers() {
        assertReadAdapterShape(
            JpaAssignmentCampaignReadRepositoryAdapter.class,
            AssignmentCampaignReadRepository.class,
            Set.of(
                SpringDataAssignmentCampaignJpaRepository.class,
                SpringDataAssignmentCampaignCourseJpaRepository.class,
                SpringDataAssignmentCampaignRecipientSnapshotJpaRepository.class,
                AssignmentPersistenceMapper.class
            )
        );
        assertReadAdapterShape(
            JpaAssignmentReadRepositoryAdapter.class,
            AssignmentReadRepository.class,
            Set.of(
                SpringDataAssignmentJpaRepository.class,
                SpringDataAssignmentTestJpaRepository.class,
                SpringDataAssignmentAdministrativeActionJpaRepository.class,
                AssignmentPersistenceMapper.class
            )
        );
        assertThat(AssignmentSelfScopedReadRepository.class.isAssignableFrom(JpaAssignmentReadRepositoryAdapter.class)).isTrue();

        assertThat(fieldTypes(JpaAssignmentCampaignReadRepositoryAdapter.class))
            .doesNotContain(
                SpringDataAssignmentAdministrativeActionJpaRepository.class,
                SpringDataAssignmentTestJpaRepository.class
            );
        assertThat(fieldTypes(JpaAssignmentReadRepositoryAdapter.class))
            .doesNotContain(
                SpringDataAssignmentCampaignJpaRepository.class,
                SpringDataAssignmentCampaignCourseJpaRepository.class,
                SpringDataAssignmentCampaignRecipientSnapshotJpaRepository.class
            );
    }

    @Test
    void campaignReadAdapterDelegatesPostLaunchRootAndCourseReadsWithoutPreviewFallback() {
        JpaAssignmentCampaignReadRepositoryAdapter adapter = new JpaAssignmentCampaignReadRepositoryAdapter(
            assignmentCampaignJpaRepository,
            assignmentCampaignCourseJpaRepository,
            assignmentCampaignRecipientSnapshotJpaRepository,
            mapper
        );
        AssignmentCampaignEntity campaignEntity = campaignEntity();
        AssignmentCampaign campaign = campaign();
        AssignmentCampaignCourseEntity campaignCourseEntity = campaignCourseEntity();
        AssignmentCampaignCourse campaignCourse = campaignCourse();
        AssignmentCampaignRecipientSnapshotEntity recipientSnapshotEntity = recipientSnapshotEntity();
        AssignmentCampaignRecipientSnapshot recipientSnapshot = recipientSnapshot();

        when(assignmentCampaignJpaRepository.findById(11L)).thenReturn(java.util.Optional.of(campaignEntity));
        when(mapper.toDomain(campaignEntity)).thenReturn(campaign);
        when(assignmentCampaignCourseJpaRepository.findAllByCampaignIdOrderByIdAsc(11L)).thenReturn(List.of(campaignCourseEntity));
        when(mapper.toAssignmentCampaignCourses(List.of(campaignCourseEntity))).thenReturn(List.of(campaignCourse));
        when(assignmentCampaignRecipientSnapshotJpaRepository.findById(51L))
            .thenReturn(java.util.Optional.of(recipientSnapshotEntity));
        when(mapper.toDomain(recipientSnapshotEntity)).thenReturn(recipientSnapshot);
        when(assignmentCampaignRecipientSnapshotJpaRepository.findAllByCampaignIdOrderByIdAsc(11L))
            .thenReturn(List.of(recipientSnapshotEntity));
        when(assignmentCampaignRecipientSnapshotJpaRepository.findAllByUserIdOrderByIdAsc(101L))
            .thenReturn(List.of(recipientSnapshotEntity));
        when(mapper.toAssignmentCampaignRecipientSnapshots(List.of(recipientSnapshotEntity)))
            .thenReturn(List.of(recipientSnapshot));

        assertThat(adapter.findAssignmentCampaignById(11L)).isEqualTo(campaign);
        assertThat(adapter.findAssignmentCampaignCoursesByCampaignId(11L)).containsExactly(campaignCourse);
        assertThat(adapter.findAssignmentCampaignRecipientSnapshotById(51L)).isEqualTo(recipientSnapshot);
        assertThat(adapter.findAssignmentCampaignRecipientSnapshotsByCampaignId(11L)).containsExactly(recipientSnapshot);
        assertThat(adapter.findAssignmentCampaignRecipientSnapshotsByUserId(101L)).containsExactly(recipientSnapshot);

        verify(assignmentCampaignJpaRepository).findById(11L);
        verify(mapper).toDomain(campaignEntity);
        verify(assignmentCampaignCourseJpaRepository).findAllByCampaignIdOrderByIdAsc(11L);
        verify(mapper).toAssignmentCampaignCourses(List.of(campaignCourseEntity));
        verify(assignmentCampaignRecipientSnapshotJpaRepository).findById(51L);
        verify(assignmentCampaignRecipientSnapshotJpaRepository).findAllByCampaignIdOrderByIdAsc(11L);
        verify(assignmentCampaignRecipientSnapshotJpaRepository).findAllByUserIdOrderByIdAsc(101L);
        verify(mapper).toDomain(recipientSnapshotEntity);
        verify(mapper, times(2)).toAssignmentCampaignRecipientSnapshots(List.of(recipientSnapshotEntity));
    }

    @Test
    void assignmentReadAdapterDelegatesOnlyPersistedAssignmentRootAndSubordinateReads() {
        JpaAssignmentReadRepositoryAdapter adapter = new JpaAssignmentReadRepositoryAdapter(
            assignmentJpaRepository,
            assignmentTestJpaRepository,
            assignmentAdministrativeActionJpaRepository,
            mapper
        );
        AssignmentEntity assignmentEntity = assignmentEntity(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        AssignmentEntity secondAssignmentEntity = assignmentEntity(32L, 11L, 101L, 302L, AssignmentStatus.OVERDUE);
        AssignmentTestEntity assignmentTestEntity = assignmentTestEntity(41L, 31L, 501L, 9001L);
        AssignmentAdministrativeActionEntity administrativeActionEntity = administrativeActionEntity(51L, 31L);
        Assignment assignment = assignment(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        Assignment overdueAssignment = assignment(32L, 11L, 101L, 302L, AssignmentStatus.OVERDUE);
        AssignmentTest assignmentTest = assignmentTest(41L, 31L, 501L, 9001L);
        AssignmentAdministrativeAction administrativeAction = administrativeAction(51L, 31L);

        when(assignmentJpaRepository.findById(31L)).thenReturn(java.util.Optional.of(assignmentEntity));
        when(mapper.toDomain(assignmentEntity)).thenReturn(assignment);
        when(assignmentJpaRepository.findAllByCampaignIdOrderByIdAsc(11L)).thenReturn(List.of(assignmentEntity, secondAssignmentEntity));
        when(assignmentJpaRepository.findAllByUserIdOrderByIdAsc(101L)).thenReturn(List.of(assignmentEntity, secondAssignmentEntity));
        when(assignmentJpaRepository.findAllByUserIdAndStatusOrderByIdAsc(101L, AssignmentStatus.OVERDUE))
            .thenReturn(List.of(secondAssignmentEntity));
        when(assignmentJpaRepository.findFirstByUserIdAndCourseIdAndCancelledAtIsNullAndClosedAtIsNullOrderByIdDesc(101L, 301L))
            .thenReturn(java.util.Optional.of(assignmentEntity));
        when(mapper.toAssignments(List.of(assignmentEntity, secondAssignmentEntity))).thenReturn(List.of(assignment, overdueAssignment));
        when(mapper.toAssignments(List.of(secondAssignmentEntity))).thenReturn(List.of(overdueAssignment));
        when(assignmentTestJpaRepository.findById(41L)).thenReturn(java.util.Optional.of(assignmentTestEntity));
        when(assignmentTestJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L)).thenReturn(List.of(assignmentTestEntity));
        when(assignmentTestJpaRepository.findByCountedResultId(9001L)).thenReturn(java.util.Optional.of(assignmentTestEntity));
        when(mapper.toDomain(assignmentTestEntity)).thenReturn(assignmentTest);
        when(mapper.toAssignmentTests(List.of(assignmentTestEntity))).thenReturn(List.of(assignmentTest));
        when(assignmentAdministrativeActionJpaRepository.findById(51L))
            .thenReturn(java.util.Optional.of(administrativeActionEntity));
        when(assignmentAdministrativeActionJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L))
            .thenReturn(List.of(administrativeActionEntity));
        when(mapper.toDomain(administrativeActionEntity)).thenReturn(administrativeAction);
        when(mapper.toAssignmentAdministrativeActions(List.of(administrativeActionEntity)))
            .thenReturn(List.of(administrativeAction));

        assertThat(adapter.findAssignmentById(31L)).isEqualTo(assignment);
        assertThat(adapter.findAssignmentsByCampaignId(11L)).containsExactly(assignment, overdueAssignment);
        assertThat(adapter.findAssignmentsByUserId(101L)).containsExactly(assignment, overdueAssignment);
        assertThat(adapter.findAssignmentsByUserIdAndStatus(101L, AssignmentStatus.OVERDUE)).containsExactly(overdueAssignment);
        assertThat(adapter.findActiveAssignmentByUserIdAndCourseId(101L, 301L)).isEqualTo(assignment);
        assertThat(adapter.findAssignmentTestById(41L)).isEqualTo(assignmentTest);
        assertThat(adapter.findAssignmentTestsByAssignmentId(31L)).containsExactly(assignmentTest);
        assertThat(adapter.findAssignmentTestByCountedResultId(9001L)).isEqualTo(assignmentTest);
        assertThat(adapter.findAssignmentAdministrativeActionById(51L)).isEqualTo(administrativeAction);
        assertThat(adapter.findAssignmentAdministrativeActionsByAssignmentId(31L)).containsExactly(administrativeAction);

        verify(assignmentJpaRepository).findById(31L);
        verify(mapper, times(2)).toDomain(assignmentEntity);
        verify(assignmentJpaRepository).findAllByCampaignIdOrderByIdAsc(11L);
        verify(assignmentJpaRepository).findAllByUserIdOrderByIdAsc(101L);
        verify(assignmentJpaRepository).findAllByUserIdAndStatusOrderByIdAsc(101L, AssignmentStatus.OVERDUE);
        verify(assignmentJpaRepository).findFirstByUserIdAndCourseIdAndCancelledAtIsNullAndClosedAtIsNullOrderByIdDesc(101L, 301L);
        verify(mapper, times(2)).toAssignments(List.of(assignmentEntity, secondAssignmentEntity));
        verify(mapper).toAssignments(List.of(secondAssignmentEntity));
        verify(assignmentTestJpaRepository).findById(41L);
        verify(assignmentTestJpaRepository).findAllByAssignmentIdOrderByIdAsc(31L);
        verify(assignmentTestJpaRepository).findByCountedResultId(9001L);
        verify(mapper, times(2)).toDomain(assignmentTestEntity);
        verify(mapper).toAssignmentTests(List.of(assignmentTestEntity));
        verify(assignmentAdministrativeActionJpaRepository).findById(51L);
        verify(assignmentAdministrativeActionJpaRepository).findAllByAssignmentIdOrderByIdAsc(31L);
        verify(mapper).toDomain(administrativeActionEntity);
        verify(mapper).toAssignmentAdministrativeActions(List.of(administrativeActionEntity));
    }

    @Test
    void selfScopedDetailDelegatesToOwnedSelectionRatherThanBroadFindByIdFallback() {
        JpaAssignmentReadRepositoryAdapter adapter = new JpaAssignmentReadRepositoryAdapter(
            assignmentJpaRepository,
            assignmentTestJpaRepository,
            assignmentAdministrativeActionJpaRepository,
            mapper
        );
        AssignmentEntity ownedEntity = assignmentEntity(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        Assignment ownedAssignment = assignment(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);

        when(assignmentJpaRepository.findByIdAndUserId(31L, 101L)).thenReturn(java.util.Optional.of(ownedEntity));
        when(mapper.toDomain(ownedEntity)).thenReturn(ownedAssignment);

        assertThat(adapter.findSelfScopedAssignmentById(101L, 31L)).isEqualTo(ownedAssignment);

        verify(assignmentJpaRepository).findByIdAndUserId(31L, 101L);
        verify(assignmentJpaRepository, times(0)).findById(31L);
    }

    @Test
    void selfScopedAssignmentTestsDelegateThroughOwnedAssignmentSelectionBeforeSubordinateRead() {
        JpaAssignmentReadRepositoryAdapter adapter = new JpaAssignmentReadRepositoryAdapter(
            assignmentJpaRepository,
            assignmentTestJpaRepository,
            assignmentAdministrativeActionJpaRepository,
            mapper
        );
        AssignmentEntity ownedEntity = assignmentEntity(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        Assignment ownedAssignment = assignment(31L, 11L, 101L, 301L, AssignmentStatus.ASSIGNED);
        AssignmentTestEntity firstTestEntity = assignmentTestEntity(41L, 31L, 501L, null);
        AssignmentTestEntity secondTestEntity = assignmentTestEntity(42L, 31L, 502L, 9002L);
        AssignmentTest firstTest = assignmentTest(41L, 31L, 501L, null);
        AssignmentTest secondTest = assignmentTest(42L, 31L, 502L, 9002L);

        when(assignmentJpaRepository.findByIdAndUserId(31L, 101L)).thenReturn(java.util.Optional.of(ownedEntity));
        when(mapper.toDomain(ownedEntity)).thenReturn(ownedAssignment);
        when(assignmentTestJpaRepository.findAllByAssignmentIdOrderByIdAsc(31L))
            .thenReturn(List.of(firstTestEntity, secondTestEntity));
        when(mapper.toAssignmentTests(List.of(firstTestEntity, secondTestEntity)))
            .thenReturn(List.of(firstTest, secondTest));

        assertThat(adapter.findSelfScopedAssignmentTestsByAssignmentId(101L, 31L))
            .containsExactly(firstTest, secondTest);

        verify(assignmentJpaRepository).findByIdAndUserId(31L, 101L);
        verify(mapper).toDomain(ownedEntity);
        verify(assignmentTestJpaRepository).findAllByAssignmentIdOrderByIdAsc(31L);
        verify(mapper).toAssignmentTests(List.of(firstTestEntity, secondTestEntity));
        verify(assignmentJpaRepository, times(0)).findById(31L);
    }

    private void assertReadAdapterShape(Class<?> adapterClass, Class<?> contractClass, Set<Class<?>> expectedFieldTypes) {
        assertThat(contractClass.isAssignableFrom(adapterClass)).isTrue();
        assertThat(adapterClass.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(adapterClass.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(adapterClass.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(fieldTypes(adapterClass)).isEqualTo(expectedFieldTypes);
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods()).map(method -> method.getName()).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields()).map(Field::getType).collect(Collectors.toUnmodifiableSet());
    }

    private AssignmentCampaign campaign() {
        return new AssignmentCampaign(11L, "Launched campaign", "read contour", "ORG_UNIT", "ou-42", "Operations", FIXED_INSTANT, FIXED_INSTANT);
    }

    private AssignmentCampaignEntity campaignEntity() {
        AssignmentCampaignEntity entity = new AssignmentCampaignEntity();
        entity.setId(11L);
        entity.setName("Launched campaign");
        entity.setDescription("read contour");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("ou-42");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignCourse campaignCourse() {
        return new AssignmentCampaignCourse(21L, 11L, 301L, FIXED_INSTANT, FIXED_INSTANT);
    }

    private AssignmentCampaignCourseEntity campaignCourseEntity() {
        AssignmentCampaignCourseEntity entity = new AssignmentCampaignCourseEntity();
        entity.setId(21L);
        entity.setCampaignId(11L);
        entity.setCourseId(301L);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private Assignment assignment(
        Long assignmentId,
        Long campaignId,
        Long userId,
        Long courseId,
        AssignmentStatus status
    ) {
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

    private AssignmentAdministrativeAction administrativeAction(Long actionId, Long assignmentId) {
        return new AssignmentAdministrativeAction(
            actionId,
            assignmentId,
            AssignmentAdministrativeActionType.REPLACE_WITH_NEW_ASSIGNMENT,
            FIXED_INSTANT,
            "typed administrative action",
            FIXED_INSTANT
        );
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

    private AssignmentCampaignRecipientSnapshot recipientSnapshot() {
        return new AssignmentCampaignRecipientSnapshot(
            51L,
            11L,
            101L,
            301L,
            "/company/division/unit",
            "ORG_UNIT",
            "EMP-001",
            "Ivan Ivanov",
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentCampaignRecipientSnapshotEntity recipientSnapshotEntity() {
        AssignmentCampaignRecipientSnapshotEntity entity = new AssignmentCampaignRecipientSnapshotEntity();
        entity.setId(51L);
        entity.setCampaignId(11L);
        entity.setUserId(101L);
        entity.setOrganizationalUnitIdSnapshot(301L);
        entity.setOrganizationalPathSnapshot("/company/division/unit");
        entity.setInclusionBasisCode("ORG_UNIT");
        entity.setEmployeeNumberSnapshot("EMP-001");
        entity.setFullNameSnapshot("Ivan Ivanov");
        entity.setCapturedAt(FIXED_INSTANT);
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }
}
