package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.vladislav.training.platform.assignment.repository.AssignmentAdministrativeActionRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignCourseRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentRepository;
import com.vladislav.training.platform.assignment.repository.AssignmentTestRepository;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
/**
 * Проверяет форму и состав {@code AssignmentOwnerRepositoryAdapter}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentOwnerRepositoryAdapterShapeTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-10T12:00:00Z");

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
    void ownerRepositoryContractsStayNarrowAndDoNotExposeForbiddenRuntimeSemantics() {
        assertThat(methodNames(AssignmentCampaignRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentCampaignById",
                "findAllAssignmentCampaigns",
                "findAssignmentCampaignsBySourceType",
                "saveAssignmentCampaign"
            )
            .doesNotContain("updateAssignmentCampaign", "deleteAssignmentCampaign", "patchAssignmentCampaign", "rewriteAssignmentCampaign");

        assertThat(methodNames(AssignmentCampaignCourseRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentCampaignCourseById",
                "findAssignmentCampaignCoursesByCampaignId",
                "saveAssignmentCampaignCourse"
            )
            .doesNotContain("removeAssignmentCampaignCourse", "rewriteCampaignComposition", "patchAssignmentCampaignCourse");

        assertThat(methodNames(AssignmentCampaignRecipientSnapshotRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentCampaignRecipientSnapshotById",
                "findAssignmentCampaignRecipientSnapshotsByCampaignId",
                "findAssignmentCampaignRecipientSnapshotsByUserId",
                "saveAssignmentCampaignRecipientSnapshot"
            )
            .doesNotContain("updateAssignmentCampaignRecipientSnapshot", "deleteAssignmentCampaignRecipientSnapshot", "replaceRecipientList");

        assertThat(methodNames(AssignmentRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentById",
                "findAllAssignments",
                "findAssignmentsByCampaignId",
                "findAssignmentsByUserId",
                "findAssignmentsByUserIdAndStatus",
                "findActiveAssignmentByUserIdAndCourseId",
                "saveAssignment"
            )
            .doesNotContain("patchAssignment", "updateStatus", "changeAssignee", "editAssignmentTests");

        assertThat(methodNames(AssignmentTestRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentTestById",
                "findAssignmentTestsByAssignmentId",
                "findAssignmentTestByCountedResultId",
                "saveAssignmentTest"
            )
            .doesNotContain("closeAssignmentTest", "recalculateAssignmentTest", "overrideCountedResult");

        assertThat(methodNames(AssignmentAdministrativeActionRepository.class))
            .containsExactlyInAnyOrder(
                "findAssignmentAdministrativeActionById",
                "findAssignmentAdministrativeActionsByAssignmentId",
                "saveAssignmentAdministrativeAction"
            )
            .doesNotContain("saveAuditEvent", "patchAssignmentAdministrativeAction", "updateAssignmentAdministrativeAction");
    }

    @Test
    void ownerAdaptersStayRepositoryAnnotatedTransactionalAndMapperBacked() {
        assertAdapterShape(
            JpaAssignmentCampaignRepositoryAdapter.class,
            AssignmentCampaignRepository.class,
            Set.of(SpringDataAssignmentCampaignJpaRepository.class, AssignmentPersistenceMapper.class)
        );
        assertAdapterShape(
            JpaAssignmentCampaignCourseRepositoryAdapter.class,
            AssignmentCampaignCourseRepository.class,
            Set.of(SpringDataAssignmentCampaignCourseJpaRepository.class, AssignmentPersistenceMapper.class)
        );
        assertAdapterShape(
            JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter.class,
            AssignmentCampaignRecipientSnapshotRepository.class,
            Set.of(SpringDataAssignmentCampaignRecipientSnapshotJpaRepository.class, AssignmentPersistenceMapper.class)
        );
        assertAdapterShape(
            JpaAssignmentRepositoryAdapter.class,
            AssignmentRepository.class,
            Set.of(SpringDataAssignmentJpaRepository.class, AssignmentPersistenceMapper.class)
        );
        assertAdapterShape(
            JpaAssignmentTestRepositoryAdapter.class,
            AssignmentTestRepository.class,
            Set.of(SpringDataAssignmentTestJpaRepository.class, AssignmentPersistenceMapper.class)
        );
        assertAdapterShape(
            JpaAssignmentAdministrativeActionRepositoryAdapter.class,
            AssignmentAdministrativeActionRepository.class,
            Set.of(SpringDataAssignmentAdministrativeActionJpaRepository.class, AssignmentPersistenceMapper.class)
        );
    }

    @Test
    void ownerSpringDataCarriersStayBoundToCanonicalEntitiesAndNarrowFinderShape() {
        assertSpringDataShape(
            SpringDataAssignmentCampaignJpaRepository.class,
            AssignmentCampaignEntity.class,
            Set.of("findAllByOrderByIdAsc", "findAllBySourceTypeOrderByIdAsc")
        );
        assertSpringDataShape(
            SpringDataAssignmentCampaignCourseJpaRepository.class,
            AssignmentCampaignCourseEntity.class,
            Set.of("findAllByCampaignIdOrderByIdAsc")
        );
        assertSpringDataShape(
            SpringDataAssignmentCampaignRecipientSnapshotJpaRepository.class,
            AssignmentCampaignRecipientSnapshotEntity.class,
            Set.of("findAllByCampaignIdOrderByIdAsc", "findAllByUserIdOrderByIdAsc")
        );
        assertSpringDataShape(
            SpringDataAssignmentJpaRepository.class,
            AssignmentEntity.class,
            Set.of(
                "findAllByOrderByIdAsc",
                "findAllByCampaignIdOrderByIdAsc",
                "findAllByUserIdOrderByIdAsc",
                "findAllByUserIdAndStatusOrderByIdAsc",
                "findByIdAndUserId",
                "findFirstByUserIdAndCourseIdAndCancelledAtIsNullAndClosedAtIsNullOrderByIdDesc"
            )
        );
        assertSpringDataShape(
            SpringDataAssignmentTestJpaRepository.class,
            AssignmentTestEntity.class,
            Set.of("findAllByAssignmentIdOrderByIdAsc", "findByCountedResultId")
        );
        assertSpringDataShape(
            SpringDataAssignmentAdministrativeActionJpaRepository.class,
            AssignmentAdministrativeActionEntity.class,
            Set.of("findAllByAssignmentIdOrderByIdAsc")
        );
    }

    @Test
    void campaignAdapterDelegatesReadAndSaveThroughSpringDataAndMapper() {
        JpaAssignmentCampaignRepositoryAdapter adapter = new JpaAssignmentCampaignRepositoryAdapter(
            assignmentCampaignJpaRepository,
            mapper
        );
        AssignmentCampaignEntity entity = campaignEntity();
        AssignmentCampaign domain = campaign();

        when(assignmentCampaignJpaRepository.findAllByOrderByIdAsc()).thenReturn(List.of(entity));
        when(mapper.toAssignmentCampaigns(List.of(entity))).thenReturn(List.of(domain));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(assignmentCampaignJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAllAssignmentCampaigns()).containsExactly(domain);
        assertThat(adapter.saveAssignmentCampaign(domain)).isEqualTo(domain);

        verify(assignmentCampaignJpaRepository).findAllByOrderByIdAsc();
        verify(mapper).toAssignmentCampaigns(List.of(entity));
        verify(mapper).toEntity(domain);
        verify(assignmentCampaignJpaRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void campaignCourseAdapterDelegatesReadAndSaveThroughSpringDataAndMapper() {
        JpaAssignmentCampaignCourseRepositoryAdapter adapter = new JpaAssignmentCampaignCourseRepositoryAdapter(
            assignmentCampaignCourseJpaRepository,
            mapper
        );
        AssignmentCampaignCourseEntity entity = campaignCourseEntity();
        AssignmentCampaignCourse domain = campaignCourse();

        when(assignmentCampaignCourseJpaRepository.findAllByCampaignIdOrderByIdAsc(11L)).thenReturn(List.of(entity));
        when(mapper.toAssignmentCampaignCourses(List.of(entity))).thenReturn(List.of(domain));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(assignmentCampaignCourseJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAssignmentCampaignCoursesByCampaignId(11L)).containsExactly(domain);
        assertThat(adapter.saveAssignmentCampaignCourse(domain)).isEqualTo(domain);

        verify(assignmentCampaignCourseJpaRepository).findAllByCampaignIdOrderByIdAsc(11L);
        verify(mapper).toAssignmentCampaignCourses(List.of(entity));
        verify(mapper).toEntity(domain);
        verify(assignmentCampaignCourseJpaRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void recipientSnapshotAdapterDelegatesImmutableSnapshotReadAndSaveThroughMapper() {
        JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter adapter =
            new JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter(
                assignmentCampaignRecipientSnapshotJpaRepository,
                mapper
            );
        AssignmentCampaignRecipientSnapshotEntity entity = recipientSnapshotEntity();
        AssignmentCampaignRecipientSnapshot domain = recipientSnapshot();

        when(assignmentCampaignRecipientSnapshotJpaRepository.findAllByCampaignIdOrderByIdAsc(11L)).thenReturn(List.of(entity));
        when(mapper.toAssignmentCampaignRecipientSnapshots(List.of(entity))).thenReturn(List.of(domain));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(assignmentCampaignRecipientSnapshotJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAssignmentCampaignRecipientSnapshotsByCampaignId(11L)).containsExactly(domain);
        assertThat(adapter.saveAssignmentCampaignRecipientSnapshot(domain)).isEqualTo(domain);

        verify(assignmentCampaignRecipientSnapshotJpaRepository).findAllByCampaignIdOrderByIdAsc(11L);
        verify(mapper).toAssignmentCampaignRecipientSnapshots(List.of(entity));
        verify(mapper).toEntity(domain);
        verify(assignmentCampaignRecipientSnapshotJpaRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void assignmentAdapterDelegatesStatusShapeTransfersWithoutRecalculationSemantics() {
        JpaAssignmentRepositoryAdapter adapter = new JpaAssignmentRepositoryAdapter(assignmentJpaRepository, mapper);
        AssignmentEntity entity = assignmentEntity();
        Assignment domain = assignment();

        when(assignmentJpaRepository.findAllByUserIdAndStatusOrderByIdAsc(201L, AssignmentStatus.OVERDUE))
            .thenReturn(List.of(entity));
        when(mapper.toAssignments(List.of(entity))).thenReturn(List.of(domain));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(assignmentJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAssignmentsByUserIdAndStatus(201L, AssignmentStatus.OVERDUE)).containsExactly(domain);
        assertThat(adapter.saveAssignment(domain)).isEqualTo(domain);

        verify(assignmentJpaRepository).findAllByUserIdAndStatusOrderByIdAsc(201L, AssignmentStatus.OVERDUE);
        verify(mapper).toAssignments(List.of(entity));
        verify(mapper).toEntity(domain);
        verify(assignmentJpaRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void assignmentTestAdapterDelegatesShapeOnlyLookupAndSaveThroughMapper() {
        JpaAssignmentTestRepositoryAdapter adapter = new JpaAssignmentTestRepositoryAdapter(assignmentTestJpaRepository, mapper);
        AssignmentTestEntity entity = assignmentTestEntity();
        AssignmentTest domain = assignmentTest();

        when(assignmentTestJpaRepository.findAllByAssignmentIdOrderByIdAsc(14L)).thenReturn(List.of(entity));
        when(mapper.toAssignmentTests(List.of(entity))).thenReturn(List.of(domain));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(assignmentTestJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAssignmentTestsByAssignmentId(14L)).containsExactly(domain);
        assertThat(adapter.saveAssignmentTest(domain)).isEqualTo(domain);

        verify(assignmentTestJpaRepository).findAllByAssignmentIdOrderByIdAsc(14L);
        verify(mapper).toAssignmentTests(List.of(entity));
        verify(mapper).toEntity(domain);
        verify(assignmentTestJpaRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void administrativeActionAdapterDelegatesTypedHistoryPersistenceWithoutAuditMixing() {
        JpaAssignmentAdministrativeActionRepositoryAdapter adapter =
            new JpaAssignmentAdministrativeActionRepositoryAdapter(assignmentAdministrativeActionJpaRepository, mapper);
        AssignmentAdministrativeActionEntity entity = administrativeActionEntity();
        AssignmentAdministrativeAction domain = administrativeAction();

        when(assignmentAdministrativeActionJpaRepository.findAllByAssignmentIdOrderByIdAsc(14L)).thenReturn(List.of(entity));
        when(mapper.toAssignmentAdministrativeActions(List.of(entity))).thenReturn(List.of(domain));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(assignmentAdministrativeActionJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAssignmentAdministrativeActionsByAssignmentId(14L)).containsExactly(domain);
        assertThat(adapter.saveAssignmentAdministrativeAction(domain)).isEqualTo(domain);

        verify(assignmentAdministrativeActionJpaRepository).findAllByAssignmentIdOrderByIdAsc(14L);
        verify(mapper).toAssignmentAdministrativeActions(List.of(entity));
        verify(mapper).toEntity(domain);
        verify(assignmentAdministrativeActionJpaRepository).save(entity);
        verify(mapper).toDomain(entity);
    }

    private void assertAdapterShape(Class<?> adapterClass, Class<?> contractClass, Set<Class<?>> expectedFieldTypes) {
        assertThat(contractClass.isAssignableFrom(adapterClass)).isTrue();
        assertThat(adapterClass.isAnnotationPresent(Repository.class)).isTrue();
        assertThat(adapterClass.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(adapterClass.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(fieldTypes(adapterClass)).isEqualTo(expectedFieldTypes);
    }

    private void assertSpringDataShape(
        Class<?> repositoryClass,
        Class<?> entityClass,
        Set<String> expectedDeclaredMethods
    ) {
        ParameterizedType jpaRepositoryType = Stream.of(repositoryClass.getGenericInterfaces())
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .filter(type -> JpaRepository.class.equals(type.getRawType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("JpaRepository generic shape is missing: " + repositoryClass));

        Type[] typeArguments = jpaRepositoryType.getActualTypeArguments();

        assertThat(typeArguments).containsExactly(entityClass, Long.class);
        assertThat(methodNames(repositoryClass))
            .containsExactlyInAnyOrderElementsOf(expectedDeclaredMethods)
            .doesNotContain("findPreviewRecipientPool", "searchAssignments", "reportAssignments", "patchAssignment", "saveAuditEvent");
    }

    private Set<String> methodNames(Class<?> type) {
        return Stream.of(type.getDeclaredMethods()).map(method -> method.getName()).collect(Collectors.toUnmodifiableSet());
    }

    private Set<Class<?>> fieldTypes(Class<?> type) {
        return Stream.of(type.getDeclaredFields()).map(Field::getType).collect(Collectors.toUnmodifiableSet());
    }

    private AssignmentCampaign campaign() {
        return new AssignmentCampaign(11L, "Campaign", "desc", "ORG_UNIT", "ou-42", "Ops", FIXED_INSTANT, FIXED_INSTANT);
    }

    private AssignmentCampaignEntity campaignEntity() {
        AssignmentCampaignEntity entity = new AssignmentCampaignEntity();
        entity.setId(11L);
        entity.setName("Campaign");
        entity.setDescription("desc");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("ou-42");
        entity.setSourceNameSnapshot("Ops");
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignCourse campaignCourse() {
        return new AssignmentCampaignCourse(12L, 11L, 101L, FIXED_INSTANT, FIXED_INSTANT);
    }

    private AssignmentCampaignCourseEntity campaignCourseEntity() {
        AssignmentCampaignCourseEntity entity = new AssignmentCampaignCourseEntity();
        entity.setId(12L);
        entity.setCampaignId(11L);
        entity.setCourseId(101L);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentCampaignRecipientSnapshot recipientSnapshot() {
        return new AssignmentCampaignRecipientSnapshot(
            13L,
            11L,
            201L,
            301L,
            "/company/ops/line-a",
            "ORG_UNIT_TARGETING",
            "E-1001",
            "Ivan Petrov",
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentCampaignRecipientSnapshotEntity recipientSnapshotEntity() {
        AssignmentCampaignRecipientSnapshotEntity entity = new AssignmentCampaignRecipientSnapshotEntity();
        entity.setId(13L);
        entity.setCampaignId(11L);
        entity.setUserId(201L);
        entity.setOrganizationalUnitIdSnapshot(301L);
        entity.setOrganizationalPathSnapshot("/company/ops/line-a");
        entity.setCapturedAt(FIXED_INSTANT);
        entity.setInclusionBasisCode("ORG_UNIT_TARGETING");
        entity.setEmployeeNumberSnapshot("E-1001");
        entity.setFullNameSnapshot("Ivan Petrov");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }

    private Assignment assignment() {
        return new Assignment(
            14L,
            11L,
            201L,
            101L,
            AssignmentStatus.OVERDUE,
            FIXED_INSTANT.minusSeconds(3600),
            FIXED_INSTANT.plusSeconds(3600),
            null,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentEntity assignmentEntity() {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setId(14L);
        entity.setCampaignId(11L);
        entity.setUserId(201L);
        entity.setCourseId(101L);
        entity.setStatus(AssignmentStatus.OVERDUE);
        entity.setAssignedAt(FIXED_INSTANT.minusSeconds(3600));
        entity.setDeadlineAt(FIXED_INSTANT.plusSeconds(3600));
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentTest assignmentTest() {
        return new AssignmentTest(
            15L,
            14L,
            401L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            501L,
            FIXED_INSTANT,
            true,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }

    private AssignmentTestEntity assignmentTestEntity() {
        AssignmentTestEntity entity = new AssignmentTestEntity();
        entity.setId(15L);
        entity.setAssignmentId(14L);
        entity.setTestId(401L);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(501L);
        entity.setClosedAt(FIXED_INSTANT);
        entity.setClosed(true);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentAdministrativeAction administrativeAction() {
        return new AssignmentAdministrativeAction(
            16L,
            14L,
            AssignmentAdministrativeActionType.EXTEND_DEADLINE,
            FIXED_INSTANT,
            "deadline extended",
            FIXED_INSTANT
        );
    }

    private AssignmentAdministrativeActionEntity administrativeActionEntity() {
        AssignmentAdministrativeActionEntity entity = new AssignmentAdministrativeActionEntity();
        entity.setId(16L);
        entity.setAssignmentId(14L);
        entity.setActionType(AssignmentAdministrativeActionType.EXTEND_DEADLINE);
        entity.setOccurredAt(FIXED_INSTANT);
        entity.setNote("deadline extended");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }
}
