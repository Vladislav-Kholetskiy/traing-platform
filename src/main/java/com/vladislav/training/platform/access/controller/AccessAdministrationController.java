package com.vladislav.training.platform.access.controller;

import com.vladislav.training.platform.access.controller.dto.AssignManagementRelationRequest;
import com.vladislav.training.platform.access.controller.dto.AssignTemporaryAccessAreaRequest;
import com.vladislav.training.platform.access.controller.dto.AssignTemporaryManagementDelegationRequest;
import com.vladislav.training.platform.access.controller.dto.AssignTemporaryRoleRequest;
import com.vladislav.training.platform.access.controller.dto.AssignUserAccessAreaRequest;
import com.vladislav.training.platform.access.controller.dto.CloseManagementRelationRequest;
import com.vladislav.training.platform.access.controller.dto.CloseTemporaryAccessAreaRequest;
import com.vladislav.training.platform.access.controller.dto.CloseTemporaryManagementDelegationRequest;
import com.vladislav.training.platform.access.controller.dto.CloseTemporaryRoleRequest;
import com.vladislav.training.platform.access.controller.dto.CloseUserAccessAreaRequest;
import com.vladislav.training.platform.access.controller.dto.ManagementRelationResponse;
import com.vladislav.training.platform.access.controller.dto.ManagementRelationTypeResponse;
import com.vladislav.training.platform.access.controller.dto.TemporaryAccessAreaResponse;
import com.vladislav.training.platform.access.controller.dto.TemporaryManagementDelegationResponse;
import com.vladislav.training.platform.access.controller.dto.TemporaryRoleAssignmentResponse;
import com.vladislav.training.platform.access.controller.dto.UserAccessAreaResponse;
import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.AccessAdministrationCommandService;
import com.vladislav.training.platform.access.service.AccessAdministrationQueryService;
import com.vladislav.training.platform.access.service.ManagementRelationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentAdminFilter;
import com.vladislav.training.platform.access.service.UserAccessAreaAdminFilter;
import com.vladislav.training.platform.common.time.UtcClock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
/**
 * Контроллер {@code AccessAdministrationController}.
 */
public class AccessAdministrationController {

    private final AccessAdministrationCommandService accessAdministrationCommandService;
    private final AccessAdministrationQueryService accessAdministrationQueryService;
    private final UtcClock utcClock;

    public AccessAdministrationController(
        AccessAdministrationCommandService accessAdministrationCommandService,
        AccessAdministrationQueryService accessAdministrationQueryService,
        UtcClock utcClock
    ) {
        this.accessAdministrationCommandService = accessAdministrationCommandService;
        this.accessAdministrationQueryService = accessAdministrationQueryService;
        this.utcClock = utcClock;
    }

    @GetMapping("/access-areas")
    public List<UserAccessAreaResponse> listAccessAreas(
        @RequestParam(required = false) @Positive Long userId,
        @RequestParam(required = false) @Positive Long organizationalUnitId,
        @RequestParam(required = false) AccessScopeType accessScopeType,
        @RequestParam(required = false) Boolean activeOnly,
        @RequestParam(required = false) Instant activeAt
    ) {
        Instant effectiveFilterAt = Boolean.TRUE.equals(activeOnly) ? (activeAt == null ? utcClock.now() : activeAt) : null;
        return accessAdministrationQueryService.listUserAccessAreas(new UserAccessAreaAdminFilter(
            userId,
            organizationalUnitId,
            accessScopeType,
            effectiveFilterAt
        )).stream().map(this::toUserAccessAreaResponse).toList();
    }

    @GetMapping("/management-relations")
    public List<ManagementRelationResponse> listManagementRelations(
        @RequestParam(required = false) @Positive Long userId,
        @RequestParam(required = false) @Positive Long organizationalUnitId,
        @RequestParam(required = false) @Positive Long managementRelationTypeId,
        @RequestParam(required = false) Boolean activeOnly,
        @RequestParam(required = false) Instant activeAt
    ) {
        Instant effectiveFilterAt = Boolean.TRUE.equals(activeOnly) ? (activeAt == null ? utcClock.now() : activeAt) : null;
        return accessAdministrationQueryService.listManagementRelations(new ManagementRelationAdminFilter(
            userId,
            organizationalUnitId,
            managementRelationTypeId,
            effectiveFilterAt
        )).stream().map(this::toManagementRelationResponse).toList();
    }

    @GetMapping("/temporary-role-assignments")
    public List<TemporaryRoleAssignmentResponse> listTemporaryRoleAssignments(
        @RequestParam(required = false) @Positive Long userId,
        @RequestParam(required = false) @Positive Long roleId,
        @RequestParam(required = false) Boolean activeOnly,
        @RequestParam(required = false) Instant activeAt
    ) {
        Instant effectiveFilterAt = Boolean.TRUE.equals(activeOnly) ? (activeAt == null ? utcClock.now() : activeAt) : null;
        return accessAdministrationQueryService.listTemporaryRoleAssignments(new TemporaryRoleAssignmentAdminFilter(
            userId,
            roleId,
            effectiveFilterAt
        )).stream().map(this::toTemporaryRoleAssignmentResponse).toList();
    }

    @GetMapping("/temporary-access-areas")
    public List<TemporaryAccessAreaResponse> listTemporaryAccessAreas(
        @RequestParam(required = false) @Positive Long userId,
        @RequestParam(required = false) @Positive Long organizationalUnitId,
        @RequestParam(required = false) AccessScopeType accessScopeType,
        @RequestParam(required = false) Boolean activeOnly,
        @RequestParam(required = false) Instant activeAt
    ) {
        Instant effectiveFilterAt = Boolean.TRUE.equals(activeOnly) ? (activeAt == null ? utcClock.now() : activeAt) : null;
        return accessAdministrationQueryService.listTemporaryAccessAreas(new TemporaryAccessAreaAdminFilter(
            userId,
            organizationalUnitId,
            accessScopeType,
            effectiveFilterAt
        )).stream().map(this::toTemporaryAccessAreaResponse).toList();
    }

    @GetMapping("/temporary-management-delegations")
    public List<TemporaryManagementDelegationResponse> listTemporaryManagementDelegations(
        @RequestParam(required = false) @Positive Long userId,
        @RequestParam(required = false) @Positive Long organizationalUnitId,
        @RequestParam(required = false) @Positive Long managementRelationTypeId,
        @RequestParam(required = false) Boolean activeOnly,
        @RequestParam(required = false) Instant activeAt
    ) {
        Instant effectiveFilterAt = Boolean.TRUE.equals(activeOnly) ? (activeAt == null ? utcClock.now() : activeAt) : null;
        return accessAdministrationQueryService.listTemporaryManagementDelegations(
            new TemporaryManagementDelegationAdminFilter(
                userId,
                organizationalUnitId,
                managementRelationTypeId,
                effectiveFilterAt
            )
        ).stream().map(this::toTemporaryManagementDelegationResponse).toList();
    }

    @GetMapping("/management-relation-types")
    public List<ManagementRelationTypeResponse> listManagementRelationTypes() {
        return accessAdministrationQueryService.listManagementRelationTypes().stream()
            .map(this::toManagementRelationTypeResponse)
            .toList();
    }

    @PostMapping("/access-areas")
    public ResponseEntity<UserAccessAreaResponse> assignUserAccessArea(
        @Valid @RequestBody AssignUserAccessAreaRequest request
    ) {
        UserAccessArea createdArea = accessAdministrationCommandService.assignUserAccessArea(
            request.userId(),
            request.organizationalUnitId(),
            request.accessScopeType(),
            request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserAccessAreaResponse(createdArea));
    }

    @PostMapping("/access-areas/{id}/close")
    public UserAccessAreaResponse closeUserAccessArea(
        @PathVariable @Positive Long id,
        @RequestBody(required = false) CloseUserAccessAreaRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toUserAccessAreaResponse(accessAdministrationCommandService.closeUserAccessArea(id, validTo));
    }

    @PostMapping("/management-relations")
    public ResponseEntity<ManagementRelationResponse> assignManagementRelation(
        @Valid @RequestBody AssignManagementRelationRequest request
    ) {
        ManagementRelation createdRelation = accessAdministrationCommandService.assignManagementRelation(
            request.userId(),
            request.organizationalUnitId(),
            request.managementRelationTypeId(),
            request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toManagementRelationResponse(createdRelation));
    }

    @PostMapping("/management-relations/{id}/close")
    public ManagementRelationResponse closeManagementRelation(
        @PathVariable @Positive Long id,
        @RequestBody(required = false) CloseManagementRelationRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toManagementRelationResponse(accessAdministrationCommandService.closeManagementRelation(id, validTo));
    }

    @PostMapping("/temporary-role-assignments")
    public ResponseEntity<TemporaryRoleAssignmentResponse> assignTemporaryRoleAssignment(
        @Valid @RequestBody AssignTemporaryRoleRequest request
    ) {
        TemporaryRoleAssignment createdAssignment = accessAdministrationCommandService.assignTemporaryRoleAssignment(
            request.userId(),
            request.roleId(),
            request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toTemporaryRoleAssignmentResponse(createdAssignment));
    }

    @PostMapping("/temporary-role-assignments/{id}/close")
    public TemporaryRoleAssignmentResponse closeTemporaryRoleAssignment(
        @PathVariable @Positive Long id,
        @RequestBody(required = false) CloseTemporaryRoleRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toTemporaryRoleAssignmentResponse(accessAdministrationCommandService.closeTemporaryRoleAssignment(id, validTo));
    }

    @PostMapping("/temporary-access-areas")
    public ResponseEntity<TemporaryAccessAreaResponse> assignTemporaryAccessArea(
        @Valid @RequestBody AssignTemporaryAccessAreaRequest request
    ) {
        TemporaryAccessArea createdArea = accessAdministrationCommandService.assignTemporaryAccessArea(
            request.userId(),
            request.organizationalUnitId(),
            request.accessScopeType(),
            request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toTemporaryAccessAreaResponse(createdArea));
    }

    @PostMapping("/temporary-access-areas/{id}/close")
    public TemporaryAccessAreaResponse closeTemporaryAccessArea(
        @PathVariable @Positive Long id,
        @RequestBody(required = false) CloseTemporaryAccessAreaRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toTemporaryAccessAreaResponse(accessAdministrationCommandService.closeTemporaryAccessArea(id, validTo));
    }

    @PostMapping("/temporary-management-delegations")
    public ResponseEntity<TemporaryManagementDelegationResponse> assignTemporaryManagementDelegation(
        @Valid @RequestBody AssignTemporaryManagementDelegationRequest request
    ) {
        TemporaryManagementDelegation createdDelegation = accessAdministrationCommandService.assignTemporaryManagementDelegation(
            request.userId(),
            request.organizationalUnitId(),
            request.managementRelationTypeId(),
            request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toTemporaryManagementDelegationResponse(createdDelegation));
    }

    @PostMapping("/temporary-management-delegations/{id}/close")
    public TemporaryManagementDelegationResponse closeTemporaryManagementDelegation(
        @PathVariable @Positive Long id,
        @RequestBody(required = false) CloseTemporaryManagementDelegationRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toTemporaryManagementDelegationResponse(
            accessAdministrationCommandService.closeTemporaryManagementDelegation(id, validTo)
        );
    }

    private UserAccessAreaResponse toUserAccessAreaResponse(UserAccessArea userAccessArea) {
        return new UserAccessAreaResponse(
            userAccessArea.id(),
            userAccessArea.userId(),
            userAccessArea.organizationalUnitId(),
            userAccessArea.accessScopeType(),
            userAccessArea.validFrom(),
            userAccessArea.validTo(),
            userAccessArea.createdAt(),
            userAccessArea.updatedAt()
        );
    }

    private ManagementRelationResponse toManagementRelationResponse(ManagementRelation managementRelation) {
        return new ManagementRelationResponse(
            managementRelation.id(),
            managementRelation.userId(),
            managementRelation.organizationalUnitId(),
            managementRelation.managementRelationTypeId(),
            managementRelation.validFrom(),
            managementRelation.validTo(),
            managementRelation.createdAt(),
            managementRelation.updatedAt()
        );
    }

    private TemporaryRoleAssignmentResponse toTemporaryRoleAssignmentResponse(
        TemporaryRoleAssignment temporaryRoleAssignment
    ) {
        return new TemporaryRoleAssignmentResponse(
            temporaryRoleAssignment.id(),
            temporaryRoleAssignment.userId(),
            temporaryRoleAssignment.roleId(),
            temporaryRoleAssignment.validFrom(),
            temporaryRoleAssignment.validTo(),
            temporaryRoleAssignment.createdAt(),
            temporaryRoleAssignment.updatedAt()
        );
    }

    private TemporaryAccessAreaResponse toTemporaryAccessAreaResponse(TemporaryAccessArea temporaryAccessArea) {
        return new TemporaryAccessAreaResponse(
            temporaryAccessArea.id(),
            temporaryAccessArea.userId(),
            temporaryAccessArea.organizationalUnitId(),
            temporaryAccessArea.accessScopeType(),
            temporaryAccessArea.validFrom(),
            temporaryAccessArea.validTo(),
            temporaryAccessArea.createdAt(),
            temporaryAccessArea.updatedAt()
        );
    }

    private TemporaryManagementDelegationResponse toTemporaryManagementDelegationResponse(
        TemporaryManagementDelegation temporaryManagementDelegation
    ) {
        return new TemporaryManagementDelegationResponse(
            temporaryManagementDelegation.id(),
            temporaryManagementDelegation.userId(),
            temporaryManagementDelegation.organizationalUnitId(),
            temporaryManagementDelegation.managementRelationTypeId(),
            temporaryManagementDelegation.validFrom(),
            temporaryManagementDelegation.validTo(),
            temporaryManagementDelegation.createdAt(),
            temporaryManagementDelegation.updatedAt()
        );
    }

    private ManagementRelationTypeResponse toManagementRelationTypeResponse(ManagementRelationType managementRelationType) {
        return new ManagementRelationTypeResponse(
            managementRelationType.id(),
            managementRelationType.code(),
            managementRelationType.name(),
            managementRelationType.description(),
            managementRelationType.createdAt(),
            managementRelationType.updatedAt()
        );
    }
}
