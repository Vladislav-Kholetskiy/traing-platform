package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignResponse;
import com.vladislav.training.platform.assignment.controller.dto.LaunchIndividualAssignmentRequest;
import com.vladislav.training.platform.assignment.domain.AssignmentCampaign;
import com.vladislav.training.platform.assignment.service.IndividualAssignmentCommandService;
import com.vladislav.training.platform.assignment.service.LaunchIndividualAssignmentCommand;
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
 * Контроллер {@code AssignmentIndividualLaunchController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assignment-campaigns")
public class AssignmentIndividualLaunchController {

    private final IndividualAssignmentCommandService individualAssignmentCommandService;

    public AssignmentIndividualLaunchController(IndividualAssignmentCommandService individualAssignmentCommandService) {
        this.individualAssignmentCommandService = Objects.requireNonNull(
            individualAssignmentCommandService,
            "individualAssignmentCommandService must not be null"
        );
    }

    @PostMapping("/launch-individual")
    public ResponseEntity<AssignmentCampaignResponse> launchIndividual(
        @Valid @RequestBody LaunchIndividualAssignmentRequest request
    ) {
        AssignmentCampaign campaign = individualAssignmentCommandService.launchIndividualAssignment(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(campaign));
    }

    private LaunchIndividualAssignmentCommand toCommand(LaunchIndividualAssignmentRequest request) {
        return new LaunchIndividualAssignmentCommand(
            request.name(),
            request.description(),
            request.userId(),
            request.courseIds(),
            new LaunchIndividualAssignmentCommand.DeadlinePolicy(request.deadlinePolicy().deadlineAt())
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
