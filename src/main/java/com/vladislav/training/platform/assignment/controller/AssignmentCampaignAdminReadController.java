package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignResponse;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignQueryService;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/assignment-campaigns")
public class AssignmentCampaignAdminReadController {

    private final AssignmentCampaignQueryService assignmentCampaignQueryService;

    public AssignmentCampaignAdminReadController(AssignmentCampaignQueryService assignmentCampaignQueryService) {
        this.assignmentCampaignQueryService = Objects.requireNonNull(
            assignmentCampaignQueryService,
            "assignmentCampaignQueryService must not be null"
        );
    }

    @GetMapping
    public List<AssignmentCampaignResponse> findAssignmentCampaigns(
        @RequestParam(required = false) String sourceType
    ) {
        List<AssignmentCampaign> campaigns = sourceType == null || sourceType.isBlank()
            ? assignmentCampaignQueryService.findAllAssignmentCampaigns()
            : assignmentCampaignQueryService.findAssignmentCampaignsBySourceType(sourceType.trim());
        return campaigns.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{campaignId}")
    public AssignmentCampaignResponse findAssignmentCampaignById(@PathVariable Long campaignId) {
        return toResponse(assignmentCampaignQueryService.findAssignmentCampaignById(campaignId));
    }

    private AssignmentCampaignResponse toResponse(AssignmentCampaign campaign) {
        return new AssignmentCampaignResponse(
            campaign.id(),
            campaign.name(),
            campaign.description(),
            campaign.sourceType(),
            campaign.sourceRef(),
            campaign.sourceNameSnapshot(),
            campaign.createdAt(),
            campaign.updatedAt()
        );
    }
}
