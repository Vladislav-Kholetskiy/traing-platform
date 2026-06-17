package com.vladislav.training.platform.assignment.infrastructure.persistence;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaAssignmentCampaignReadRepositoryAdapter implements AssignmentCampaignReadRepository {

    private final SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository;
    private final SpringDataAssignmentCampaignCourseJpaRepository assignmentCampaignCourseRepository;
    private final SpringDataAssignmentCampaignRecipientSnapshotJpaRepository assignmentCampaignRecipientSnapshotRepository;
    private final AssignmentPersistenceMapper mapper;

    public JpaAssignmentCampaignReadRepositoryAdapter(
        SpringDataAssignmentCampaignJpaRepository assignmentCampaignRepository,
        SpringDataAssignmentCampaignCourseJpaRepository assignmentCampaignCourseRepository,
        SpringDataAssignmentCampaignRecipientSnapshotJpaRepository assignmentCampaignRecipientSnapshotRepository,
        AssignmentPersistenceMapper mapper
    ) {
        this.assignmentCampaignRepository = assignmentCampaignRepository;
        this.assignmentCampaignCourseRepository = assignmentCampaignCourseRepository;
        this.assignmentCampaignRecipientSnapshotRepository = assignmentCampaignRecipientSnapshotRepository;
        this.mapper = mapper;
    }

    @Override
    public AssignmentCampaign findAssignmentCampaignById(Long assignmentCampaignId) {
        return assignmentCampaignRepository.findById(assignmentCampaignId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Assignment campaign not found: " + assignmentCampaignId));
    }

    @Override
    public List<AssignmentCampaign> findAllAssignmentCampaigns() {
        return mapper.toAssignmentCampaigns(assignmentCampaignRepository.findAllByOrderByIdAsc());
    }

    @Override
    public List<AssignmentCampaign> findAssignmentCampaignsBySourceType(String sourceType) {
        return mapper.toAssignmentCampaigns(
            assignmentCampaignRepository.findAllBySourceTypeOrderByIdAsc(sourceType)
        );
    }

    @Override
    public AssignmentCampaignCourse findAssignmentCampaignCourseById(Long assignmentCampaignCourseId) {
        return assignmentCampaignCourseRepository.findById(assignmentCampaignCourseId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException(
                "Assignment campaign course not found: " + assignmentCampaignCourseId
            ));
    }

    @Override
    public List<AssignmentCampaignCourse> findAssignmentCampaignCoursesByCampaignId(Long assignmentCampaignId) {
        return mapper.toAssignmentCampaignCourses(
            assignmentCampaignCourseRepository.findAllByCampaignIdOrderByIdAsc(assignmentCampaignId)
        );
    }

    @Override
    public AssignmentCampaignRecipientSnapshot findAssignmentCampaignRecipientSnapshotById(Long recipientSnapshotId) {
        return assignmentCampaignRecipientSnapshotRepository.findById(recipientSnapshotId)
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
            assignmentCampaignRecipientSnapshotRepository.findAllByCampaignIdOrderByIdAsc(assignmentCampaignId)
        );
    }

    @Override
    public List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByUserId(Long userId) {
        return mapper.toAssignmentCampaignRecipientSnapshots(
            assignmentCampaignRecipientSnapshotRepository.findAllByUserIdOrderByIdAsc(userId)
        );
    }
}
