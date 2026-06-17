package com.vladislav.training.platform.userorg.controller;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.controller.dto.AssignOrganizationUnitRequest;
import com.vladislav.training.platform.userorg.controller.dto.AssignRoleRequest;
import com.vladislav.training.platform.userorg.controller.dto.CloseOrganizationAssignmentRequest;
import com.vladislav.training.platform.userorg.controller.dto.CloseRoleAssignmentRequest;
import com.vladislav.training.platform.userorg.controller.dto.CreateUserRequest;
import com.vladislav.training.platform.userorg.controller.dto.ReplacePrimaryHomeUnitRequest;
import com.vladislav.training.platform.userorg.controller.dto.RoleResponse;
import com.vladislav.training.platform.userorg.controller.dto.UpdateUserRequest;
import com.vladislav.training.platform.userorg.controller.dto.UserCardResponse;
import com.vladislav.training.platform.userorg.controller.dto.UserOrganizationAssignmentResponse;
import com.vladislav.training.platform.userorg.controller.dto.UserResponse;
import com.vladislav.training.platform.userorg.controller.dto.UserRoleAssignmentResponse;
import com.vladislav.training.platform.userorg.domain.AppRole;
import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.domain.UserRoleAssignment;
import com.vladislav.training.platform.userorg.domain.UserStatus;
import com.vladislav.training.platform.userorg.service.UserAdministrationCard;
import com.vladislav.training.platform.userorg.service.UserAdministrationCommandService;
import com.vladislav.training.platform.userorg.service.UserAdministrationOrganizationAssignmentView;
import com.vladislav.training.platform.userorg.service.UserAdministrationQueryService;
import com.vladislav.training.platform.userorg.service.UserAdministrationRoleAssignmentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер {@code UserAdministrationController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/admin")
public class UserAdministrationController {

    private final UserAdministrationCommandService userAdministrationCommandService;
    private final UserAdministrationQueryService userAdministrationQueryService;
    private final UtcClock utcClock;

    public UserAdministrationController(
            UserAdministrationCommandService userAdministrationCommandService,
            UserAdministrationQueryService userAdministrationQueryService,
            UtcClock utcClock
    ) {
        this.userAdministrationCommandService = userAdministrationCommandService;
        this.userAdministrationQueryService = userAdministrationQueryService;
        this.utcClock = utcClock;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers(@RequestParam(required = false) UserStatus status) {
        return userAdministrationQueryService.listUsers(status).stream()
                .map(this::toUserResponse)
                .toList();
    }

    @GetMapping("/users/{id}")
    public UserCardResponse getUserCard(@PathVariable @Positive Long id) {
        return toUserCardResponse(userAdministrationQueryService.getUserCard(id));
    }

    @GetMapping("/users/{id}/roles")
    public List<UserRoleAssignmentResponse> getRoleHistory(@PathVariable @Positive Long id) {
        return userAdministrationQueryService.getRoleHistory(id).stream()
                .map(this::toUserRoleAssignmentResponse)
                .toList();
    }

    @GetMapping("/users/{id}/organization-assignments")
    public List<UserOrganizationAssignmentResponse> getOrganizationAssignmentHistory(@PathVariable @Positive Long id) {
        return userAdministrationQueryService.getOrganizationAssignmentHistory(id).stream()
                .map(this::toUserOrganizationAssignmentResponse)
                .toList();
    }

    @GetMapping("/roles")
    public List<RoleResponse> listRoles() {
        return userAdministrationQueryService.listRoles().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        Instant now = utcClock.now();
        AppUser createdUser = userAdministrationCommandService.createUser(new AppUser(
                null,
                normalizeRequired(request.employeeNumber()),
                normalizeOptionalExternalId(request.externalId()),
                normalizeRequired(request.lastName()),
                normalizeRequired(request.firstName()),
                normalizeOptional(request.middleName()),
                request.status(),
                now,
                now
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(createdUser));
    }

    @PatchMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable @Positive Long id, @Valid @RequestBody UpdateUserRequest request) {
        AppUser updatedUser = userAdministrationCommandService.updateUser(
                id,
                normalizeRequired(request.lastName()),
                normalizeRequired(request.firstName()),
                normalizeOptional(request.middleName())
        );
        return toUserResponse(updatedUser);
    }

    @PostMapping("/users/{id}/deactivate")
    public UserResponse deactivateUser(@PathVariable @Positive Long id) {
        return toUserResponse(userAdministrationCommandService.deactivateUser(id));
    }

    @PostMapping("/users/{id}/roles")
    public ResponseEntity<UserRoleAssignmentResponse> assignRole(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        UserRoleAssignment createdAssignment = userAdministrationCommandService.assignRole(
                id,
                request.roleId(),
                request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                toUserRoleAssignmentResponse(userAdministrationQueryService.toRoleAssignmentView(createdAssignment))
        );
    }

    @PostMapping("/users/{id}/roles/{assignmentId}/close")
    public UserRoleAssignmentResponse closeRole(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long assignmentId,
            @RequestBody(required = false) CloseRoleAssignmentRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toUserRoleAssignmentResponse(
                userAdministrationQueryService.toRoleAssignmentView(
                        userAdministrationCommandService.closeRole(id, assignmentId, validTo)
                )
        );
    }

    @PostMapping("/users/{id}/organization-assignments")
    public ResponseEntity<UserOrganizationAssignmentResponse> assignOrganizationAssignment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AssignOrganizationUnitRequest request
    ) {
        UserOrganizationAssignment createdAssignment = userAdministrationCommandService.assignOrganizationAssignment(
                id,
                request.organizationalUnitId(),
                request.assignmentType(),
                request.validFrom() == null ? utcClock.now() : request.validFrom()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                toUserOrganizationAssignmentResponse(
                        userAdministrationQueryService.toOrganizationAssignmentView(createdAssignment)
                )
        );
    }

    @PostMapping("/users/{id}/organization-assignments/{assignmentId}/close")
    public UserOrganizationAssignmentResponse closeOrganizationAssignment(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long assignmentId,
            @RequestBody(required = false) CloseOrganizationAssignmentRequest request
    ) {
        Instant validTo = request == null || request.validTo() == null ? utcClock.now() : request.validTo();
        return toUserOrganizationAssignmentResponse(
                userAdministrationQueryService.toOrganizationAssignmentView(
                        userAdministrationCommandService.closeOrganizationAssignment(id, assignmentId, validTo)
                )
        );
    }

    @PostMapping("/users/{id}/primary-home-unit/replace")
    public UserOrganizationAssignmentResponse replacePrimaryHomeUnit(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ReplacePrimaryHomeUnitRequest request
    ) {
        Instant effectiveAt = request.effectiveAt() == null ? utcClock.now() : request.effectiveAt();
        return toUserOrganizationAssignmentResponse(
                userAdministrationQueryService.toOrganizationAssignmentView(
                        userAdministrationCommandService.replacePrimaryHomeUnit(id, request.organizationalUnitId(), effectiveAt)
                )
        );
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.id(),
                user.employeeNumber(),
                user.externalId(),
                user.lastName(),
                user.firstName(),
                user.middleName(),
                user.status(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    private UserCardResponse toUserCardResponse(UserAdministrationCard card) {
        AppUser user = card.user();
        return new UserCardResponse(
                user.id(),
                user.employeeNumber(),
                user.externalId(),
                user.lastName(),
                user.firstName(),
                user.middleName(),
                user.status(),
                user.createdAt(),
                user.updatedAt(),
                card.activeRoleAssignments().stream().map(this::toUserRoleAssignmentResponse).toList(),
                card.activeOrganizationAssignments().stream().map(this::toUserOrganizationAssignmentResponse).toList()
        );
    }

    private UserRoleAssignmentResponse toUserRoleAssignmentResponse(UserAdministrationRoleAssignmentView assignment) {
        return new UserRoleAssignmentResponse(
                assignment.id(),
                assignment.userId(),
                assignment.roleId(),
                assignment.roleCode(),
                assignment.roleName(),
                assignment.validFrom(),
                assignment.validTo(),
                assignment.createdAt(),
                assignment.updatedAt()
        );
    }

    private UserOrganizationAssignmentResponse toUserOrganizationAssignmentResponse(
            UserAdministrationOrganizationAssignmentView assignment
    ) {
        return new UserOrganizationAssignmentResponse(
                assignment.id(),
                assignment.userId(),
                assignment.organizationalUnitId(),
                assignment.organizationalUnitName(),
                assignment.organizationalUnitPath(),
                assignment.assignmentType(),
                assignment.validFrom(),
                assignment.validTo(),
                assignment.createdAt(),
                assignment.updatedAt()
        );
    }

    private RoleResponse toRoleResponse(AppRole role) {
        return new RoleResponse(role.id(), role.code(), role.name(), role.description(), role.createdAt(), role.updatedAt());
    }

    private String normalizeRequired(String value) {
        String normalizedValue = normalizeOptional(value);
        if (normalizedValue == null) {
            throw new ValidationException("Request field must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeOptionalExternalId(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.isBlank()) {
            throw new ValidationException("externalId must not be blank when provided");
        }
        return normalizedValue;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }
}