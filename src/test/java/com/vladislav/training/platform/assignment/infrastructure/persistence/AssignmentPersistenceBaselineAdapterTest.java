package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AssignmentPersistenceBaselineAdapter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentPersistenceBaselineAdapterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-08T18:00:00Z");

    @Mock
    private SpringDataAssignmentCampaignRecipientSnapshotJpaRepository snapshotJpaRepository;
    @Mock
    private SpringDataAssignmentTestJpaRepository assignmentTestJpaRepository;
    @Mock
    private SpringDataAssignmentAdministrativeActionJpaRepository administrativeActionJpaRepository;

    private final AssignmentPersistenceMapper mapper = new AssignmentPersistenceMapper();

    @Test
    void recipientSnapshotAdapterMapsCampaignScopedReadAndSave() {
        JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter adapter =
            new JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter(snapshotJpaRepository, mapper);
        AssignmentCampaignRecipientSnapshotEntity entity = recipientSnapshotEntity(11L, 21L, 31L);

        when(snapshotJpaRepository.findAllByCampaignIdOrderByIdAsc(21L)).thenReturn(List.of(entity));
        when(snapshotJpaRepository.save(any(AssignmentCampaignRecipientSnapshotEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.findAssignmentCampaignRecipientSnapshotsByCampaignId(21L))
            .singleElement()
            .satisfies(snapshot -> {
                assertThat(snapshot.id()).isEqualTo(11L);
                assertThat(snapshot.campaignId()).isEqualTo(21L);
                assertThat(snapshot.userId()).isEqualTo(31L);
            });

        AssignmentCampaignRecipientSnapshot saved = adapter.saveAssignmentCampaignRecipientSnapshot(
            mapper.toDomain(entity)
        );

        assertThat(saved.id()).isEqualTo(11L);
        verify(snapshotJpaRepository).save(any(AssignmentCampaignRecipientSnapshotEntity.class));
    }

    @Test
    void recipientSnapshotShapeRemainsImmutableWithoutUpdatedAt() {
        AssignmentCampaignRecipientSnapshot snapshot = mapper.toDomain(recipientSnapshotEntity(11L, 21L, 31L));
        AssignmentCampaignRecipientSnapshotEntity entity = mapper.toEntity(snapshot);

        assertThat(Arrays.stream(AssignmentCampaignRecipientSnapshot.class.getRecordComponents())
            .map(component -> component.getName())).doesNotContain("updatedAt");
        assertThat(Stream.of(AssignmentCampaignRecipientSnapshotEntity.class.getDeclaredFields())
            .map(field -> field.getName())).doesNotContain("updatedAt");
        assertThat(snapshot.capturedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(snapshot.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(entity.getCapturedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(entity.getCreatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void assignmentTestAdapterMapsCountedResultLookupAndSave() {
        JpaAssignmentTestRepositoryAdapter adapter =
            new JpaAssignmentTestRepositoryAdapter(assignmentTestJpaRepository, mapper);
        AssignmentTestEntity entity = assignmentTestEntity(12L, 22L, 32L, 42L);

        when(assignmentTestJpaRepository.findByCountedResultId(42L)).thenReturn(Optional.of(entity));
        when(assignmentTestJpaRepository.save(any(AssignmentTestEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentTest found = adapter.findAssignmentTestByCountedResultId(42L);
        AssignmentTest saved = adapter.saveAssignmentTest(mapper.toDomain(entity));

        assertThat(found.id()).isEqualTo(12L);
        assertThat(found.countedResultId()).isEqualTo(42L);
        assertThat(saved.id()).isEqualTo(12L);
        verify(assignmentTestJpaRepository).save(any(AssignmentTestEntity.class));
    }

    @Test
    void assignmentTestAdapterFailsClosedWhenCountedResultRowIsMissing() {
        JpaAssignmentTestRepositoryAdapter adapter =
            new JpaAssignmentTestRepositoryAdapter(assignmentTestJpaRepository, mapper);

        when(assignmentTestJpaRepository.findByCountedResultId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findAssignmentTestByCountedResultId(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void administrativeActionAdapterMapsAssignmentScopedReadAndSave() {
        JpaAssignmentAdministrativeActionRepositoryAdapter adapter =
            new JpaAssignmentAdministrativeActionRepositoryAdapter(administrativeActionJpaRepository, mapper);
        AssignmentAdministrativeActionEntity entity = administrativeActionEntity(13L, 23L);

        when(administrativeActionJpaRepository.findAllByAssignmentIdOrderByIdAsc(23L)).thenReturn(List.of(entity));
        when(administrativeActionJpaRepository.save(any(AssignmentAdministrativeActionEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.findAssignmentAdministrativeActionsByAssignmentId(23L))
            .singleElement()
            .satisfies(action -> {
                assertThat(action.id()).isEqualTo(13L);
                assertThat(action.assignmentId()).isEqualTo(23L);
                assertThat(action.actionType()).isEqualTo(AssignmentAdministrativeActionType.EXTEND_DEADLINE);
            });

        AssignmentAdministrativeAction saved = adapter.saveAssignmentAdministrativeAction(mapper.toDomain(entity));

        assertThat(saved.id()).isEqualTo(13L);
        verify(administrativeActionJpaRepository).save(any(AssignmentAdministrativeActionEntity.class));
    }

    private AssignmentCampaignRecipientSnapshotEntity recipientSnapshotEntity(Long id, Long campaignId, Long userId) {
        AssignmentCampaignRecipientSnapshotEntity entity = new AssignmentCampaignRecipientSnapshotEntity();
        entity.setId(id);
        entity.setCampaignId(campaignId);
        entity.setUserId(userId);
        entity.setOrganizationalUnitIdSnapshot(41L);
        entity.setOrganizationalPathSnapshot("/ou/root/child");
        entity.setCapturedAt(FIXED_INSTANT);
        entity.setInclusionBasisCode("ORG_UNIT_TARGETING");
        entity.setEmployeeNumberSnapshot("E-1001");
        entity.setFullNameSnapshot("Snapshot User");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentTestEntity assignmentTestEntity(Long id, Long assignmentId, Long testId, Long countedResultId) {
        AssignmentTestEntity entity = new AssignmentTestEntity();
        entity.setId(id);
        entity.setAssignmentId(assignmentId);
        entity.setTestId(testId);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(countedResultId);
        entity.setClosedAt(FIXED_INSTANT);
        entity.setClosed(true);
        entity.setCreatedAt(FIXED_INSTANT);
        entity.setUpdatedAt(FIXED_INSTANT);
        return entity;
    }

    private AssignmentAdministrativeActionEntity administrativeActionEntity(Long id, Long assignmentId) {
        AssignmentAdministrativeActionEntity entity = new AssignmentAdministrativeActionEntity();
        entity.setId(id);
        entity.setAssignmentId(assignmentId);
        entity.setActionType(AssignmentAdministrativeActionType.EXTEND_DEADLINE);
        entity.setOccurredAt(FIXED_INSTANT);
        entity.setNote("deadline extended for readiness test");
        entity.setCreatedAt(FIXED_INSTANT);
        return entity;
    }
}
