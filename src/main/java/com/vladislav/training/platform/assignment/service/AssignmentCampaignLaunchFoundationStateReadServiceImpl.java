package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.repository.AssignmentCampaignRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@ConditionalOnBean(AssignmentCampaignRepository.class)
class AssignmentCampaignLaunchFoundationStateReadServiceImpl
    implements AssignmentCampaignLaunchFoundationStateReadService {

    private final AssignmentCampaignRepository assignmentCampaignRepository;

    AssignmentCampaignLaunchFoundationStateReadServiceImpl(AssignmentCampaignRepository assignmentCampaignRepository) {
        this.assignmentCampaignRepository = assignmentCampaignRepository;
    }

    @Override
    public AssignmentCampaignLaunchFoundationState findAssignmentCampaignLaunchFoundationState(
        AssignmentCampaignLaunchAdmissionAnchor launchAdmissionAnchor
    ) {
        Objects.requireNonNull(launchAdmissionAnchor, "launchAdmissionAnchor must not be null");
        String sourceType = requireSourceType(launchAdmissionAnchor.sourceType());
        String sourceRef = normalizeSourceRef(launchAdmissionAnchor.sourceRef());

        if (sourceRef == null) {
            return new AssignmentCampaignLaunchFoundationState(false);
        }

        List<AssignmentCampaign> campaigns = Objects.requireNonNull(
            assignmentCampaignRepository.findAssignmentCampaignsBySourceType(sourceType),
            "AssignmentCampaignRepository returned null campaigns"
        );

        boolean sourceAnchorAlreadyMaterialized = false;
        for (AssignmentCampaign campaign : campaigns) {
            if (sourceRef.equals(normalizeSourceRef(campaign.sourceRef()))) {
                sourceAnchorAlreadyMaterialized = true;
                break;
            }
        }

        return new AssignmentCampaignLaunchFoundationState(sourceAnchorAlreadyMaterialized);
    }

    private String requireSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType must not be blank");
        }
        return sourceType;
    }

    private String normalizeSourceRef(String sourceRef) {
        if (sourceRef == null || sourceRef.isBlank()) {
            return null;
        }
        return sourceRef;
    }
}
