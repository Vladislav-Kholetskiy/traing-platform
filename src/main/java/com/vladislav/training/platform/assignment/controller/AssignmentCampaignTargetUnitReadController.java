package com.vladislav.training.platform.assignment.controller;

import com.vladislav.training.platform.assignment.controller.dto.AssignmentCampaignTargetUnitResponse;
import com.vladislav.training.platform.assignment.service.AssignmentCampaignTargetingReadService;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code AssignmentCampaignTargetUnitReadController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/assignment-campaigns")
public class AssignmentCampaignTargetUnitReadController {

    private final AssignmentCampaignTargetingReadService assignmentCampaignTargetingReadService;

    public AssignmentCampaignTargetUnitReadController(
        AssignmentCampaignTargetingReadService assignmentCampaignTargetingReadService
    ) {
        this.assignmentCampaignTargetingReadService = assignmentCampaignTargetingReadService;
    }

    @GetMapping("/target-units")
    public List<AssignmentCampaignTargetUnitResponse> findAvailableTargetUnits() {
        return assignmentCampaignTargetingReadService.findAvailableTargetUnits().stream()
            .map(unit -> new AssignmentCampaignTargetUnitResponse(unit.id(), unit.name(), unit.path()))
            .toList();
    }
}
