package com.vladislav.training.platform.integration.personnel.controller;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.integration.personnel.controller.dto.PersonnelApplyResponse;
import com.vladislav.training.platform.integration.personnel.controller.dto.PersonnelDryRunResponse;
import com.vladislav.training.platform.integration.personnel.controller.dto.PersonnelDryRunRowResult;
import com.vladislav.training.platform.integration.personnel.controller.dto.PersonnelPlannedMutationDto;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.service.PersonnelApplyService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelImportAdmissionService;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
/**
 * Контроллер {@code PersonnelExcelImportController}.
 */

@RestController
@RequestMapping("/api/v1/admin/import/personnel-excel")
public class PersonnelExcelImportController {

    private final PersonnelImportAdmissionService personnelImportAdmissionService;
    private final PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade;
    private final PersonnelApplyService personnelApplyService;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    public PersonnelExcelImportController(
        PersonnelImportAdmissionService personnelImportAdmissionService,
        PersonnelWorkbookDryRunFacade personnelWorkbookDryRunFacade,
        PersonnelApplyService personnelApplyService,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
    ) {
        this.personnelImportAdmissionService = Objects.requireNonNull(
            personnelImportAdmissionService,
            "personnelImportAdmissionService must not be null"
        );
        this.personnelWorkbookDryRunFacade = Objects.requireNonNull(
            personnelWorkbookDryRunFacade,
            "personnelWorkbookDryRunFacade must not be null"
        );
        this.personnelApplyService = Objects.requireNonNull(
            personnelApplyService,
            "personnelApplyService must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
    }

    @PostMapping("/dry-run")
    public ResponseEntity<PersonnelDryRunResponse> dryRun(@RequestParam(name = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }

        CapabilityAdmissionRequest admissionRequest = capabilityAdmissionRequestFactory.createPersonnelExcelDryRun();
        personnelImportAdmissionService.checkDryRunAdmission(admissionRequest);

        return ResponseEntity.ok(new PersonnelDryRunResponse(
            personnelWorkbookDryRunFacade.dryRun(readBytes(file)).stream()
                .map(this::toRowResult)
                .toList()
        ));
    }

    @PostMapping("/apply")
    public ResponseEntity<PersonnelApplyResponse> apply(@RequestParam(name = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }

        PersonnelApplyResult result = personnelApplyService.apply(readBytes(file));
        return ResponseEntity.ok(new PersonnelApplyResponse(
            result.rows().stream()
                .map(row -> new com.vladislav.training.platform.integration.personnel.controller.dto.PersonnelApplyRowResult(
                    row.rowNumber(),
                    row.employeeNumber(),
                    row.outcomeCode(),
                    row.decision(),
                    row.targetUserStatus(),
                    row.issues(),
                    row.appliedMutationTypes(),
                    row.createdUserId()
                ))
                .toList()
        ));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded workbook", e);
        }
    }

    private PersonnelDryRunRowResult toRowResult(PersonnelPlan plan) {
        return new PersonnelDryRunRowResult(
            plan.rowNumber(),
            plan.employeeNumber(),
            plan.outcomeCode().name(),
            plan.decision().name(),
            plan.targetUserStatus(),
            plan.issues(),
            plan.plannedMutations().stream()
                .map(this::toMutationDto)
                .toList()
        );
    }

    private PersonnelPlannedMutationDto toMutationDto(PersonnelPlannedMutation mutation) {
        return new PersonnelPlannedMutationDto(
            mutation.mutationType().name(),
            mutation.targetRef(),
            mutation.detail()
        );
    }
}
