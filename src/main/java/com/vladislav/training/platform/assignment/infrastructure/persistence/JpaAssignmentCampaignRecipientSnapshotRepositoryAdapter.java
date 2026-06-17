package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRecipientSnapshotRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter
    implements AssignmentCampaignRecipientSnapshotRepository {

    private final SpringDataAssignmentCampaignRecipientSnapshotJpaRepository repository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentCampaignRecipientSnapshotRepositoryAdapter(
        SpringDataAssignmentCampaignRecipientSnapshotJpaRepository repository,
        AssignmentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentCampaignRecipientSnapshot findAssignmentCampaignRecipientSnapshotById(Long recipientSnapshotId) {
        return repository.findById(recipientSnapshotId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment campaign recipient snapshot not found: " + recipientSnapshotId
            ));
    }

    @Override
    public List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByCampaignId(
        Long assignmentCampaignId
    ) {
        return mapper.toAssignmentCampaignRecipientSnapshots(
            repository.findAllByCampaignIdOrderByIdAsc(assignmentCampaignId)
        );
    }

    @Override
    public List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByUserId(Long userId) {
        return mapper.toAssignmentCampaignRecipientSnapshots(repository.findAllByUserIdOrderByIdAsc(userId));
    }

    @Override
    @Transactional
    public AssignmentCampaignRecipientSnapshot saveAssignmentCampaignRecipientSnapshot(
        AssignmentCampaignRecipientSnapshot assignmentCampaignRecipientSnapshot
    ) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(assignmentCampaignRecipientSnapshot)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException(
                "Failed to persist assignment_campaign_recipient_snapshot",
                exception
            );
        }
    }
}
