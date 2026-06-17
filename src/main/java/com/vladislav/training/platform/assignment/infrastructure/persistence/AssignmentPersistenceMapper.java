package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.Assignment;
import com.vladislav.training.platform.assignment.domain.AssignmentAdministrativeAction;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Преобразователь {@code AssignmentPersistenceMapper}.
 */
@Component
public class AssignmentPersistenceMapper {

    public AssignmentCampaign toDomain(AssignmentCampaignEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssignmentCampaign(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getSourceType(),
            entity.getSourceRef(),
            entity.getSourceNameSnapshot(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AssignmentCampaignEntity toEntity(AssignmentCampaign domain) {
        if (domain == null) {
            return null;
        }
        AssignmentCampaignEntity entity = new AssignmentCampaignEntity();
        entity.setId(domain.id());
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        entity.setSourceType(domain.sourceType());
        entity.setSourceRef(domain.sourceRef());
        entity.setSourceNameSnapshot(domain.sourceNameSnapshot());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<AssignmentCampaign> toAssignmentCampaigns(List<AssignmentCampaignEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public AssignmentCampaignCourse toDomain(AssignmentCampaignCourseEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssignmentCampaignCourse(
            entity.getId(),
            entity.getCampaignId(),
            entity.getCourseId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AssignmentCampaignCourseEntity toEntity(AssignmentCampaignCourse domain) {
        if (domain == null) {
            return null;
        }
        AssignmentCampaignCourseEntity entity = new AssignmentCampaignCourseEntity();
        entity.setId(domain.id());
        entity.setCampaignId(domain.campaignId());
        entity.setCourseId(domain.courseId());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<AssignmentCampaignCourse> toAssignmentCampaignCourses(List<AssignmentCampaignCourseEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public AssignmentCampaignRecipientSnapshot toDomain(AssignmentCampaignRecipientSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssignmentCampaignRecipientSnapshot(
            entity.getId(),
            entity.getCampaignId(),
            entity.getUserId(),
            entity.getOrganizationalUnitIdSnapshot(),
            entity.getOrganizationalPathSnapshot(),
            entity.getInclusionBasisCode(),
            entity.getEmployeeNumberSnapshot(),
            entity.getFullNameSnapshot(),
            entity.getCapturedAt(),
            entity.getCreatedAt()
        );
    }

    public AssignmentCampaignRecipientSnapshotEntity toEntity(AssignmentCampaignRecipientSnapshot domain) {
        if (domain == null) {
            return null;
        }
        AssignmentCampaignRecipientSnapshotEntity entity = new AssignmentCampaignRecipientSnapshotEntity();
        entity.setId(domain.id());
        entity.setCampaignId(domain.campaignId());
        entity.setUserId(domain.userId());
        entity.setOrganizationalUnitIdSnapshot(domain.organizationalUnitIdSnapshot());
        entity.setOrganizationalPathSnapshot(domain.organizationalPathSnapshot());
        entity.setCapturedAt(domain.capturedAt());
        entity.setInclusionBasisCode(domain.inclusionBasisCode());
        entity.setEmployeeNumberSnapshot(domain.employeeNumberSnapshot());
        entity.setFullNameSnapshot(domain.fullNameSnapshot());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public List<AssignmentCampaignRecipientSnapshot> toAssignmentCampaignRecipientSnapshots(
        List<AssignmentCampaignRecipientSnapshotEntity> entities
    ) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public Assignment toDomain(AssignmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Assignment(
            entity.getId(),
            entity.getCampaignId(),
            entity.getUserId(),
            entity.getCourseId(),
            entity.getStatus(),
            entity.getAssignedAt(),
            entity.getDeadlineAt(),
            entity.getCancelledAt(),
            entity.getClosedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AssignmentEntity toEntity(Assignment domain) {
        if (domain == null) {
            return null;
        }
        AssignmentEntity entity = new AssignmentEntity();
        entity.setId(domain.id());
        entity.setCampaignId(domain.campaignId());
        entity.setUserId(domain.userId());
        entity.setCourseId(domain.courseId());
        entity.setStatus(domain.status());
        entity.setAssignedAt(domain.assignedAt());
        entity.setDeadlineAt(domain.deadlineAt());
        entity.setCancelledAt(domain.cancelledAt());
        entity.setClosedAt(domain.closedAt());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<Assignment> toAssignments(List<AssignmentEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public AssignmentTest toDomain(AssignmentTestEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssignmentTest(
            entity.getId(),
            entity.getAssignmentId(),
            entity.getTestId(),
            entity.getAssignmentTestRole(),
            entity.getCountedResultId(),
            entity.getClosedAt(),
            entity.isClosed(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AssignmentTestEntity toEntity(AssignmentTest domain) {
        if (domain == null) {
            return null;
        }
        AssignmentTestEntity entity = new AssignmentTestEntity();
        entity.setId(domain.id());
        entity.setAssignmentId(domain.assignmentId());
        entity.setTestId(domain.testId());
        entity.setAssignmentTestRole(domain.assignmentTestRole());
        entity.setCountedResultId(domain.countedResultId());
        entity.setClosedAt(domain.closedAt());
        entity.setClosed(domain.isClosed());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    public List<AssignmentTest> toAssignmentTests(List<AssignmentTestEntity> entities) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }

    public AssignmentAdministrativeAction toDomain(AssignmentAdministrativeActionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssignmentAdministrativeAction(
            entity.getId(),
            entity.getAssignmentId(),
            entity.getActionType(),
            entity.getOccurredAt(),
            entity.getNote(),
            entity.getCreatedAt()
        );
    }

    public AssignmentAdministrativeActionEntity toEntity(AssignmentAdministrativeAction domain) {
        if (domain == null) {
            return null;
        }
        AssignmentAdministrativeActionEntity entity = new AssignmentAdministrativeActionEntity();
        entity.setId(domain.id());
        entity.setAssignmentId(domain.assignmentId());
        entity.setActionType(domain.actionType());
        entity.setOccurredAt(domain.occurredAt());
        entity.setNote(domain.note());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public List<AssignmentAdministrativeAction> toAssignmentAdministrativeActions(
        List<AssignmentAdministrativeActionEntity> entities
    ) {
        return entities == null ? List.of() : entities.stream().map(this::toDomain).toList();
    }
}
