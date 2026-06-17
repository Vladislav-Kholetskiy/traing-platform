package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignResponse;
import com.vladislav.training.platform.assignment.controller.dto.LaunchAssignmentCampaignRequest;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignCommandService;
import com.vladislav.training.platform.assignment.service.LaunchAssignmentCampaignCommand;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AssignmentCampaignLaunchController}.
 */
@Validated
@RestController
@RequestMapping("/api/v1/assignment-campaigns")
public class AssignmentCampaignLaunchController {

    private final AssignmentCampaignCommandService assignmentCampaignCommandService;

    public AssignmentCampaignLaunchController(AssignmentCampaignCommandService assignmentCampaignCommandService) {
        this.assignmentCampaignCommandService = Objects.requireNonNull(
            assignmentCampaignCommandService,
            "assignmentCampaignCommandService must not be null"
        );
    }

    @PostMapping("/launch")
    public ResponseEntity<AssignmentCampaignResponse> launchCampaign(
        @Valid @RequestBody LaunchAssignmentCampaignRequest request
    ) {
        AssignmentCampaign launchedCampaign = assignmentCampaignCommandService.launchAssignmentCampaign(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(launchedCampaign));
    }

    private LaunchAssignmentCampaignCommand toCommand(LaunchAssignmentCampaignRequest request) {
        return new LaunchAssignmentCampaignCommand(
            request.name(),
            request.description(),
            request.sourceType(),
            request.sourceRef(),
            request.sourceNameSnapshot(),
            request.courseIds(),
            new LaunchAssignmentCampaignCommand.Targeting(
                request.targeting().basisType(),
                request.targeting().basisRef()
            ),
            new LaunchAssignmentCampaignCommand.DeadlinePolicy(
                request.deadlinePolicy().deadlineAt()
            )
        );
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
