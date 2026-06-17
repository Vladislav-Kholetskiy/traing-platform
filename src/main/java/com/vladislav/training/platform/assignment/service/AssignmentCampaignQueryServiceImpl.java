package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignCourse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaignRecipientSnapshot;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignReadRepository;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code AssignmentCampaignQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnBean(AssignmentCampaignReadRepository.class)
class AssignmentCampaignQueryServiceImpl implements AssignmentCampaignQueryService {

    private static final String NOT_AUTHORIZED = "ACTOR_NOT_AUTHORIZED";
    private final AssignmentCampaignReadRepository assignmentCampaignReadRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    AssignmentCampaignQueryServiceImpl(
        AssignmentCampaignReadRepository assignmentCampaignReadRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.assignmentCampaignReadRepository = assignmentCampaignReadRepository;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
    }

    @Override
    public AssignmentCampaign findAssignmentCampaignById(Long assignmentCampaignId) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.DETAIL, "assignment_campaign");
        return assignmentCampaignReadRepository.findAssignmentCampaignById(assignmentCampaignId);
    }

    @Override
    public List<AssignmentCampaign> findAllAssignmentCampaigns() {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.LIST, "assignment_campaign");
        return assignmentCampaignReadRepository.findAllAssignmentCampaigns();
    }

    @Override
    public List<AssignmentCampaign> findAssignmentCampaignsBySourceType(String sourceType) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.LIST, "assignment_campaign");
        return assignmentCampaignReadRepository.findAssignmentCampaignsBySourceType(sourceType);
    }

    @Override
    public AssignmentCampaignCourse findAssignmentCampaignCourseById(Long assignmentCampaignCourseId) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.DETAIL, "assignment_campaign_course");
        return assignmentCampaignReadRepository.findAssignmentCampaignCourseById(assignmentCampaignCourseId);
    }

    @Override
    public List<AssignmentCampaignCourse> findAssignmentCampaignCoursesByCampaignId(Long assignmentCampaignId) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.LIST, "assignment_campaign_course");
        return assignmentCampaignReadRepository.findAssignmentCampaignCoursesByCampaignId(assignmentCampaignId);
    }

    @Override
    public AssignmentCampaignRecipientSnapshot findAssignmentCampaignRecipientSnapshotById(Long recipientSnapshotId) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.DETAIL, "assignment_campaign");
        return assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotById(recipientSnapshotId);
    }

    @Override
    public List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByCampaignId(Long assignmentCampaignId) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.LIST, "assignment_campaign");
        return assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotsByCampaignId(assignmentCampaignId);
    }

    @Override
    public List<AssignmentCampaignRecipientSnapshot> findAssignmentCampaignRecipientSnapshotsByUserId(Long userId) {
        ensurePostLaunchCampaignReadAllowed(AccessReadType.LIST, "assignment_campaign");
        return assignmentCampaignReadRepository.findAssignmentCampaignRecipientSnapshotsByUserId(userId);
    }

    private void ensurePostLaunchCampaignReadAllowed(AccessReadType readType, String targetEntityFamily) {
        if (!accessSpecificationPolicy.canRead(
            contextResolver.resolve(AccessReadArea.ASSIGNMENT_CAMPAIGN, readType, targetEntityFamily)
        )) {
            throw new PolicyViolationException(
                NOT_AUTHORIZED,
                "Actor is not authorized to read post-launch assignment campaign data"
            );
        }
    }
}
