package com.vladislav.training.platform.userorg.controller;

import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.controller.dto.CreateOrganizationalUnitRequest;
import com.vladislav.training.platform.userorg.controller.dto.CreateOrganizationalUnitTypeRequest;
import com.vladislav.training.platform.userorg.controller.dto.MoveOrganizationalUnitRequest;
import com.vladislav.training.platform.userorg.controller.dto.OrganizationalUnitResponse;
import com.vladislav.training.platform.userorg.controller.dto.OrganizationalUnitTypeResponse;
import com.vladislav.training.platform.userorg.controller.dto.UpdateOrganizationalUnitRequest;
import com.vladislav.training.platform.userorg.controller.dto.UpdateOrganizationalUnitTypeRequest;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.service.OrganizationCommandService;
import com.vladislav.training.platform.userorg.service.OrganizationPolicyReadFacade;
import com.vladislav.training.platform.userorg.service.UpdateOrganizationalUnitCommand;
import com.vladislav.training.platform.userorg.service.UpdateOrganizationalUnitTypeCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Контроллер {@code OrganizationController}.
 */
@RestController
@Validated
@RequestMapping("/api/v1/admin")
public class OrganizationController {

    private final OrganizationPolicyReadFacade organizationPolicyReadFacade;
    private final OrganizationCommandService organizationCommandService;
    private final UtcClock utcClock;

    public OrganizationController(
        OrganizationPolicyReadFacade organizationPolicyReadFacade,
        OrganizationCommandService organizationCommandService,
        UtcClock utcClock
    ) {
        this.organizationPolicyReadFacade = organizationPolicyReadFacade;
        this.organizationCommandService = organizationCommandService;
        this.utcClock = utcClock;
    }

    @GetMapping("/org-unit-types")
    public List<OrganizationalUnitTypeResponse> findOrganizationalUnitTypes(
        @RequestParam(required = false) @Positive Long id,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) OrganizationalNodeKind nodeKind
    ) {
        rejectBlankQueryValue(code, "code");
        ensureSingleFilter(
            countPresent(id, code, nodeKind),
            "Only zero or one query filter is supported for /api/v1/admin/org-unit-types"
        );

        List<OrganizationalUnitType> unitTypes;
        if (id != null) {
            unitTypes = List.of(organizationPolicyReadFacade.findOrganizationalUnitTypeById(id));
        } else if (code != null) {
            unitTypes = List.of(
                organizationPolicyReadFacade.findOrganizationalUnitTypeByCode(normalizeRequiredQueryValue(code, "code"))
            );
        } else if (nodeKind != null) {
            unitTypes = organizationPolicyReadFacade.findUnitTypesByNodeKind(nodeKind);
        } else {
            unitTypes = organizationPolicyReadFacade.findAllOrganizationalUnitTypes();
        }

        return unitTypes.stream()
            .map(this::toOrganizationalUnitTypeResponse)
            .toList();
    }

    @GetMapping("/org-units/tree")
    public List<OrganizationalUnitResponse> findOrganizationalUnitTree(
        @RequestParam(required = false) OrganizationalUnitStatus status
    ) {
        return buildTreeResponses(organizationPolicyReadFacade.findOrganizationalUnitTree(status));
    }

    @GetMapping("/org-units/{id}")
    public OrganizationalUnitResponse findOrganizationalUnitById(@PathVariable @Positive Long id) {
        return toOrganizationalUnitResponse(organizationPolicyReadFacade.findOrganizationalUnitById(id));
    }

    @GetMapping("/org-units")
    public List<OrganizationalUnitResponse> findOrganizationalUnits(
        @RequestParam(required = false) @Positive Long parentId,
        @RequestParam(required = false) String path,
        @RequestParam(required = false) OrganizationalUnitStatus status
    ) {
        rejectBlankQueryValue(path, "path");
        ensureSingleFilter(
            countPresent(parentId, path, status),
            "Only zero or one query filter is supported for /api/v1/admin/org-units"
        );

        List<OrganizationalUnit> units;
        if (parentId != null) {
            units = organizationPolicyReadFacade.findChildUnits(parentId);
        } else if (path != null) {
            units = List.of(
                organizationPolicyReadFacade.findOrganizationalUnitByPath(
                    normalizeRequiredQueryValue(path, "path")
                )
            );
        } else {
            units = organizationPolicyReadFacade.findOrganizationalUnits(status);
        }

        return units.stream()
            .map(this::toOrganizationalUnitResponse)
            .toList();
    }

    @PostMapping("/org-unit-types")
    public ResponseEntity<OrganizationalUnitTypeResponse> createOrganizationalUnitType(
        @Valid @RequestBody CreateOrganizationalUnitTypeRequest request
    ) {
        OrganizationalUnitType createdUnitType = organizationCommandService.createOrganizationalUnitType(
            toNewOrganizationalUnitType(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toOrganizationalUnitTypeResponse(createdUnitType));
    }

    @PatchMapping("/org-unit-types/{id}")
    public OrganizationalUnitTypeResponse updateOrganizationalUnitType(
        @PathVariable @Positive Long id,
        @Valid @RequestBody UpdateOrganizationalUnitTypeRequest request
    ) {
        OrganizationalUnitType updatedUnitType = organizationCommandService.updateOrganizationalUnitType(
            new UpdateOrganizationalUnitTypeCommand(
                id,
                normalizeRequiredBodyValue(request.name()),
                normalizeOptionalBodyValue(request.description()),
                request.nodeKind(),
                request.canBeOperatorHomeUnit(),
                request.canBeCampaignTarget(),
                request.participatesInSubtreeScope(),
                request.canHaveManagementRelation(),
                request.canHaveAccessArea()
            )
        );
        return toOrganizationalUnitTypeResponse(updatedUnitType);
    }

    @PostMapping("/org-units")
    public ResponseEntity<OrganizationalUnitResponse> createOrganizationalUnit(
        @Valid @RequestBody CreateOrganizationalUnitRequest request
    ) {
        OrganizationalUnit createdUnit = organizationCommandService.createOrganizationalUnit(
            toNewOrganizationalUnit(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toOrganizationalUnitResponse(createdUnit));
    }

    @PatchMapping("/org-units/{id}")
    public OrganizationalUnitResponse updateOrganizationalUnit(
        @PathVariable @Positive Long id,
        @Valid @RequestBody UpdateOrganizationalUnitRequest request
    ) {
        OrganizationalUnit updatedUnit = organizationCommandService.updateOrganizationalUnit(
            new UpdateOrganizationalUnitCommand(
                id,
                normalizeRequiredBodyValue(request.name()),
                normalizeOptionalBodyValue(request.externalId()),
                request.organizationalUnitTypeId()
            )
        );
        return toOrganizationalUnitResponse(updatedUnit);
    }

    @PostMapping("/org-units/{id}/move")
    public OrganizationalUnitResponse moveOrganizationalUnit(
        @PathVariable @Positive Long id,
        @Valid @RequestBody MoveOrganizationalUnitRequest request
    ) {
        OrganizationalUnit movedUnit = organizationCommandService.moveOrganizationalUnit(
            id,
            request.newParentOrganizationalUnitId()
        );
        return toOrganizationalUnitResponse(movedUnit);
    }

    @PostMapping("/org-units/{id}/archive")
    public OrganizationalUnitResponse archiveOrganizationalUnit(@PathVariable @Positive Long id) {
        return toOrganizationalUnitResponse(organizationCommandService.archiveOrganizationalUnit(id));
    }

    private OrganizationalUnitType toNewOrganizationalUnitType(CreateOrganizationalUnitTypeRequest request) {
        Instant now = utcClock.now();
        return new OrganizationalUnitType(
            null,
            normalizeRequiredBodyValue(request.code()),
            normalizeRequiredBodyValue(request.name()),
            normalizeOptionalBodyValue(request.description()),
            request.nodeKind(),
            requireBooleanRequestField(request.canBeOperatorHomeUnit(), "canBeOperatorHomeUnit"),
            requireBooleanRequestField(request.canBeCampaignTarget(), "canBeCampaignTarget"),
            requireBooleanRequestField(request.participatesInSubtreeScope(), "participatesInSubtreeScope"),
            requireBooleanRequestField(request.canHaveManagementRelation(), "canHaveManagementRelation"),
            requireBooleanRequestField(request.canHaveAccessArea(), "canHaveAccessArea"),
            now,
            now
        );
    }

    private OrganizationalUnit toNewOrganizationalUnit(CreateOrganizationalUnitRequest request) {
        Instant now = utcClock.now();
        return new OrganizationalUnit(
            null,
            request.parentId(),
            request.organizationalUnitTypeId(),
            normalizeRequiredBodyValue(request.name()),
            OrganizationalUnitStatus.ACTIVE,
            "/pending",
            0,
            normalizeOptionalBodyValue(request.externalId()),
            now,
            now
        );
    }

    private OrganizationalUnitResponse toOrganizationalUnitResponse(OrganizationalUnit organizationalUnit) {
        return new OrganizationalUnitResponse(
            organizationalUnit.id(),
            organizationalUnit.parentId(),
            organizationalUnit.organizationalUnitTypeId(),
            organizationalUnit.name(),
            organizationalUnit.status(),
            organizationalUnit.path(),
            organizationalUnit.depth(),
            organizationalUnit.externalId(),
            organizationalUnit.createdAt(),
            organizationalUnit.updatedAt(),
            List.of()
        );
    }

    private OrganizationalUnitResponse toTreeResponse(
        OrganizationalUnit organizationalUnit,
        Map<Long, List<OrganizationalUnit>> childrenByParent
    ) {
        List<OrganizationalUnitResponse> childResponses = childrenByParent.getOrDefault(organizationalUnit.id(), List.of())
            .stream()
            .map(child -> toTreeResponse(child, childrenByParent))
            .toList();
        return new OrganizationalUnitResponse(
            organizationalUnit.id(),
            organizationalUnit.parentId(),
            organizationalUnit.organizationalUnitTypeId(),
            organizationalUnit.name(),
            organizationalUnit.status(),
            organizationalUnit.path(),
            organizationalUnit.depth(),
            organizationalUnit.externalId(),
            organizationalUnit.createdAt(),
            organizationalUnit.updatedAt(),
            childResponses
        );
    }

    private OrganizationalUnitTypeResponse toOrganizationalUnitTypeResponse(OrganizationalUnitType organizationalUnitType) {
        return new OrganizationalUnitTypeResponse(
            organizationalUnitType.id(),
            organizationalUnitType.code(),
            organizationalUnitType.name(),
            organizationalUnitType.description(),
            organizationalUnitType.nodeKind(),
            organizationalUnitType.canBeOperatorHomeUnit(),
            organizationalUnitType.canBeCampaignTarget(),
            organizationalUnitType.participatesInSubtreeScope(),
            organizationalUnitType.canHaveManagementRelation(),
            organizationalUnitType.canHaveAccessArea(),
            organizationalUnitType.createdAt(),
            organizationalUnitType.updatedAt()
        );
    }

    private List<OrganizationalUnitResponse> buildTreeResponses(List<OrganizationalUnit> units) {
        Map<Long, List<OrganizationalUnit>> childrenByParent = new LinkedHashMap<>();
        List<OrganizationalUnit> orderedUnits = units.stream()
            .sorted(
                Comparator.comparing(
                    OrganizationalUnit::parentId,
                    Comparator.nullsFirst(Long::compareTo)
                ).thenComparing(OrganizationalUnit::id)
            )
            .toList();
        Set<Long> visibleUnitIds = new HashSet<>();

        for (OrganizationalUnit unit : orderedUnits) {
            visibleUnitIds.add(unit.id());
            childrenByParent.computeIfAbsent(unit.parentId(), ignored -> new ArrayList<>()).add(unit);
        }

        return orderedUnits.stream()
            .filter(unit -> unit.parentId() == null || !visibleUnitIds.contains(unit.parentId()))
            .map(root -> toTreeResponse(root, childrenByParent))
            .toList();
    }

    private void ensureSingleFilter(int presentFilterCount, String message) {
        if (presentFilterCount > 1) {
            throw new ValidationException(message);
        }
    }

    private int countPresent(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value instanceof String stringValue) {
                if (!stringValue.isBlank()) {
                    count++;
                }
            } else if (value != null) {
                count++;
            }
        }
        return count;
    }

    private void rejectBlankQueryValue(String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new ValidationException(fieldName + " query parameter must not be blank");
        }
    }

    private String normalizeRequiredQueryValue(String value, String fieldName) {
        String normalizedValue = normalizeOptionalBodyValue(value);
        if (normalizedValue == null) {
            throw new ValidationException(fieldName + " query parameter must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeRequiredBodyValue(String value) {
        String normalizedValue = normalizeOptionalBodyValue(value);
        if (normalizedValue == null) {
            throw new ValidationException("Request field must not be blank");
        }
        return normalizedValue;
    }

    private boolean requireBooleanRequestField(Boolean value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " request field must not be null");
        }
        return value;
    }

    private String normalizeOptionalBodyValue(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }
}


