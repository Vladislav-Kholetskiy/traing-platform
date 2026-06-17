package com.vladislav.training.platform.assignment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeActionType;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentStatus;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.assignment.domain.AssignmentTestRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
/**
 * Проверяет форму и состав {@code AssignmentPersistenceMapper}.
 * Такой тест полезен там, где важно не сломать структуру данных или типов.
 */
class AssignmentPersistenceMapperShapeTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-10T07:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-10T08:00:00Z");
    private static final Instant CAPTURED_AT = Instant.parse("2026-04-10T08:05:00Z");
    private static final Instant OCCURRED_AT = Instant.parse("2026-04-10T09:00:00Z");
    private static final Instant ASSIGNED_AT = Instant.parse("2026-04-10T06:00:00Z");
    private static final Instant DEADLINE_AT = Instant.parse("2026-04-12T06:00:00Z");
    private static final Instant CLOSED_AT = Instant.parse("2026-04-10T10:00:00Z");

    private final AssignmentPersistenceMapper mapper = new AssignmentPersistenceMapper();

    @Test
    void mapperCarriesAllCanonicalFieldsFromDomainToEntityAcrossAssignmentFamily() {
        assertThat(mapper.toEntity(assignmentCampaign()))
            .usingRecursiveComparison()
            .isEqualTo(assignmentCampaignEntity());
        assertThat(mapper.toEntity(assignmentCampaignCourse()))
            .usingRecursiveComparison()
            .isEqualTo(assignmentCampaignCourseEntity());
        assertThat(mapper.toEntity(assignmentCampaignRecipientSnapshot()))
            .usingRecursiveComparison()
            .isEqualTo(assignmentCampaignRecipientSnapshotEntity());
        assertThat(mapper.toEntity(assignment()))
            .usingRecursiveComparison()
            .isEqualTo(assignmentEntity());
        assertThat(mapper.toEntity(assignmentTest()))
            .usingRecursiveComparison()
            .isEqualTo(assignmentTestEntity());
        assertThat(mapper.toEntity(administrativeAction()))
            .usingRecursiveComparison()
            .isEqualTo(administrativeActionEntity());
    }

    @Test
    void mapperCarriesAllCanonicalFieldsFromEntityToDomainAcrossAssignmentFamily() {
        assertThat(mapper.toDomain(assignmentCampaignEntity())).isEqualTo(assignmentCampaign());
        assertThat(mapper.toDomain(assignmentCampaignCourseEntity())).isEqualTo(assignmentCampaignCourse());
        assertThat(mapper.toDomain(assignmentCampaignRecipientSnapshotEntity())).isEqualTo(assignmentCampaignRecipientSnapshot());
        assertThat(mapper.toDomain(assignmentEntity())).isEqualTo(assignment());
        assertThat(mapper.toDomain(assignmentTestEntity())).isEqualTo(assignmentTest());
        assertThat(mapper.toDomain(administrativeActionEntity())).isEqualTo(administrativeAction());
    }

    @Test
    void mapperPreservesSnapshotAdministrativeActionAndStatusFieldsWithoutBusinessLogic() {
        AssignmentCampaignRecipientSnapshotEntity snapshotEntity = mapper.toEntity(assignmentCampaignRecipientSnapshot());
        assertThat(snapshotEntity.getCapturedAt()).isEqualTo(CAPTURED_AT);
        assertThat(snapshotEntity.getCreatedAt()).isEqualTo(CREATED_AT);

        AssignmentAdministrativeActionEntity administrativeActionEntity = mapper.toEntity(administrativeAction());
        assertThat(administrativeActionEntity.getActionType()).isEqualTo(AssignmentAdministrativeActionType.EXTEND_DEADLINE);
        assertThat(administrativeActionEntity.getNote()).isEqualTo("deadline extended");
        assertThat(administrativeActionEntity.getCreatedAt()).isEqualTo(OCCURRED_AT);

        AssignmentEntity assignmentEntity = mapper.toEntity(assignment());
        assertThat(assignmentEntity.getStatus()).isEqualTo(AssignmentStatus.OVERDUE);
        assertThat(assignmentEntity.getCancelledAt()).isNull();
        assertThat(assignmentEntity.getClosedAt()).isNull();

        AssignmentTestEntity assignmentTestEntity = mapper.toEntity(assignmentTest());
        assertThat(assignmentTestEntity.getAssignmentTestRole()).isEqualTo(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        assertThat(assignmentTestEntity.getCountedResultId()).isEqualTo(501L);
        assertThat(assignmentTestEntity.getClosedAt()).isEqualTo(CLOSED_AT);
        assertThat(assignmentTestEntity.isClosed()).isTrue();
    }

    @Test
    void mapperRemainsCompileSafeForNullInputsAndListHelpers() {
        assertThat(AssignmentPersistenceMapper.class.isAnnotationPresent(Component.class)).isTrue();

        assertThat(mapper.toDomain((AssignmentCampaignEntity) null)).isNull();
        assertThat(mapper.toEntity((AssignmentCampaign) null)).isNull();
        assertThat(mapper.toAssignmentCampaigns(null)).isEmpty();

        assertThat(mapper.toDomain((AssignmentCampaignCourseEntity) null)).isNull();
        assertThat(mapper.toEntity((AssignmentCampaignCourse) null)).isNull();
        assertThat(mapper.toAssignmentCampaignCourses(null)).isEmpty();

        assertThat(mapper.toDomain((AssignmentCampaignRecipientSnapshotEntity) null)).isNull();
        assertThat(mapper.toEntity((AssignmentCampaignRecipientSnapshot) null)).isNull();
        assertThat(mapper.toAssignmentCampaignRecipientSnapshots(null)).isEmpty();

        assertThat(mapper.toDomain((AssignmentEntity) null)).isNull();
        assertThat(mapper.toEntity((Assignment) null)).isNull();
        assertThat(mapper.toAssignments(null)).isEmpty();

        assertThat(mapper.toDomain((AssignmentTestEntity) null)).isNull();
        assertThat(mapper.toEntity((AssignmentTest) null)).isNull();
        assertThat(mapper.toAssignmentTests(null)).isEmpty();

        assertThat(mapper.toDomain((AssignmentAdministrativeActionEntity) null)).isNull();
        assertThat(mapper.toEntity((AssignmentAdministrativeAction) null)).isNull();
        assertThat(mapper.toAssignmentAdministrativeActions(null)).isEmpty();
    }

    private AssignmentCampaign assignmentCampaign() {
        return new AssignmentCampaign(
            11L,
            "Кампания назначений",
            "launch-ready",
            "ORG_UNIT",
            "unit-42",
            "Operations",
            CREATED_AT,
            UPDATED_AT
        );
    }

    private AssignmentCampaignEntity assignmentCampaignEntity() {
        AssignmentCampaignEntity entity = new AssignmentCampaignEntity();
        entity.setId(11L);
        entity.setName("Кампания назначений");
        entity.setDescription("launch-ready");
        entity.setSourceType("ORG_UNIT");
        entity.setSourceRef("unit-42");
        entity.setSourceNameSnapshot("Operations");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private AssignmentCampaignCourse assignmentCampaignCourse() {
        return new AssignmentCampaignCourse(12L, 11L, 101L, CREATED_AT, UPDATED_AT);
    }

    private AssignmentCampaignCourseEntity assignmentCampaignCourseEntity() {
        AssignmentCampaignCourseEntity entity = new AssignmentCampaignCourseEntity();
        entity.setId(12L);
        entity.setCampaignId(11L);
        entity.setCourseId(101L);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private AssignmentCampaignRecipientSnapshot assignmentCampaignRecipientSnapshot() {
        return new AssignmentCampaignRecipientSnapshot(
            13L,
            11L,
            201L,
            301L,
            "/company/ops/line-a",
            "ORG_UNIT_TARGETING",
            "E-1001",
            "Ivan Petrov",
            CAPTURED_AT,
            CREATED_AT
        );
    }

    private AssignmentCampaignRecipientSnapshotEntity assignmentCampaignRecipientSnapshotEntity() {
        AssignmentCampaignRecipientSnapshotEntity entity = new AssignmentCampaignRecipientSnapshotEntity();
        entity.setId(13L);
        entity.setCampaignId(11L);
        entity.setUserId(201L);
        entity.setOrganizationalUnitIdSnapshot(301L);
        entity.setOrganizationalPathSnapshot("/company/ops/line-a");
        entity.setInclusionBasisCode("ORG_UNIT_TARGETING");
        entity.setEmployeeNumberSnapshot("E-1001");
        entity.setFullNameSnapshot("Ivan Petrov");
        entity.setCapturedAt(CAPTURED_AT);
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }

    private Assignment assignment() {
        return new Assignment(
            14L,
            11L,
            201L,
            101L,
            AssignmentStatus.OVERDUE,
            ASSIGNED_AT,
            DEADLINE_AT,
            null,
            null,
            CREATED_AT,
            UPDATED_AT
        );
    }

    private AssignmentEntity assignmentEntity() {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setId(14L);
        entity.setCampaignId(11L);
        entity.setUserId(201L);
        entity.setCourseId(101L);
        entity.setStatus(AssignmentStatus.OVERDUE);
        entity.setAssignedAt(ASSIGNED_AT);
        entity.setDeadlineAt(DEADLINE_AT);
        entity.setCancelledAt(null);
        entity.setClosedAt(null);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private AssignmentTest assignmentTest() {
        return new AssignmentTest(
            15L,
            14L,
            401L,
            AssignmentTestRole.FINAL_TOPIC_CONTROL,
            501L,
            CLOSED_AT,
            true,
            CREATED_AT,
            UPDATED_AT
        );
    }

    private AssignmentTestEntity assignmentTestEntity() {
        AssignmentTestEntity entity = new AssignmentTestEntity();
        entity.setId(15L);
        entity.setAssignmentId(14L);
        entity.setTestId(401L);
        entity.setAssignmentTestRole(AssignmentTestRole.FINAL_TOPIC_CONTROL);
        entity.setCountedResultId(501L);
        entity.setClosedAt(CLOSED_AT);
        entity.setClosed(true);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }

    private AssignmentAdministrativeAction administrativeAction() {
        return new AssignmentAdministrativeAction(
            16L,
            14L,
            AssignmentAdministrativeActionType.EXTEND_DEADLINE,
            OCCURRED_AT,
            "deadline extended",
            OCCURRED_AT
        );
    }

    private AssignmentAdministrativeActionEntity administrativeActionEntity() {
        AssignmentAdministrativeActionEntity entity = new AssignmentAdministrativeActionEntity();
        entity.setId(16L);
        entity.setAssignmentId(14L);
        entity.setActionType(AssignmentAdministrativeActionType.EXTEND_DEADLINE);
        entity.setOccurredAt(OCCURRED_AT);
        entity.setNote("deadline extended");
        entity.setCreatedAt(OCCURRED_AT);
        return entity;
    }
}
