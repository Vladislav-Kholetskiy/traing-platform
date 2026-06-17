package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionPayload;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionPolicy;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.application.policy.CapabilityTargetEntityType;
import com.vladislav.training.platform.audit.domain.AuditContext;
import com.vladislav.training.platform.audit.domain.AuditEventType;
import com.vladislav.training.platform.audit.service.CriticalCommandAuditSupport;
import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация командного сервиса {@code OrganizationCommandServiceImpl}.
 */
@Service
@Transactional
public class OrganizationCommandServiceImpl implements OrganizationCommandService {

    private static final String TARGET_MODULE = "userorg";
    private static final String ENTITY_TYPE_ORGANIZATIONAL_UNIT = "organizational_unit";
    private static final String ENTITY_TYPE_ORGANIZATIONAL_UNIT_TYPE = "organizational_unit_type";
    private static final String OPERATION_CREATE_UNIT_TYPE = "USERORG_ORGANIZATIONAL_UNIT_TYPE_CREATE";
    private static final String OPERATION_UPDATE_UNIT_TYPE = "USERORG_ORGANIZATIONAL_UNIT_TYPE_UPDATE";
    private static final String OPERATION_CREATE_UNIT = "USERORG_ORGANIZATIONAL_UNIT_CREATE";
    private static final String OPERATION_UPDATE_UNIT = "USERORG_ORGANIZATIONAL_UNIT_UPDATE";
    private static final String OPERATION_MOVE_UNIT = "USERORG_ORGANIZATIONAL_UNIT_MOVE";
    private static final String OPERATION_ARCHIVE_UNIT = "USERORG_ORGANIZATIONAL_UNIT_ARCHIVE";


    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final OrganizationalUnitTypeRepository organizationalUnitTypeRepository;
    private final CapabilityAdmissionPolicy capabilityAdmissionPolicy;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;
    private final CriticalCommandAuditSupport criticalCommandAuditSupport;
    private final UtcClock utcClock;
    private final OrganizationalUnitTreePathBuilder organizationalUnitTreePathBuilder;
    private final OrganizationalUnitSubtreeRebuilder organizationalUnitSubtreeRebuilder;
    private final OrganizationalUnitMoveValidator organizationalUnitMoveValidator;
    private final OrganizationalUnitSemanticMutationValidationSupport organizationalUnitSemanticMutationValidationSupport;
    private final OrganizationalUnitStructuralMutationValidationSupport organizationalUnitStructuralMutationValidationSupport;

    public OrganizationCommandServiceImpl(
            OrganizationalUnitRepository organizationalUnitRepository,
            OrganizationalUnitTypeRepository organizationalUnitTypeRepository,
            CapabilityAdmissionPolicy capabilityAdmissionPolicy,
            CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
            CriticalCommandAuditSupport criticalCommandAuditSupport,
            UtcClock utcClock,
            OrganizationalUnitTreePathBuilder organizationalUnitTreePathBuilder,
            OrganizationalUnitSubtreeRebuilder organizationalUnitSubtreeRebuilder,
            OrganizationalUnitMoveValidator organizationalUnitMoveValidator,
            OrganizationalUnitSemanticMutationValidationSupport organizationalUnitSemanticMutationValidationSupport,
            OrganizationalUnitStructuralMutationValidationSupport organizationalUnitStructuralMutationValidationSupport
    ) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.organizationalUnitTypeRepository = organizationalUnitTypeRepository;
        this.capabilityAdmissionPolicy = capabilityAdmissionPolicy;
        this.capabilityAdmissionRequestFactory = capabilityAdmissionRequestFactory;
        this.criticalCommandAuditSupport = criticalCommandAuditSupport;
        this.utcClock = utcClock;
        this.organizationalUnitTreePathBuilder = organizationalUnitTreePathBuilder;
        this.organizationalUnitSubtreeRebuilder = organizationalUnitSubtreeRebuilder;
        this.organizationalUnitMoveValidator = organizationalUnitMoveValidator;
        this.organizationalUnitSemanticMutationValidationSupport = organizationalUnitSemanticMutationValidationSupport;
        this.organizationalUnitStructuralMutationValidationSupport = organizationalUnitStructuralMutationValidationSupport;
    }

    @Override
    public OrganizationalUnitType createOrganizationalUnitType(OrganizationalUnitType organizationalUnitType) {
        requireIdAbsent(organizationalUnitType.id(), "organizationalUnitType");
        String normalizedCode = normalizeRequiredText(
                organizationalUnitType.code(),
                "organizationalUnitType.code must not be blank"
        );
        String normalizedName = normalizeRequiredText(
                organizationalUnitType.name(),
                "organizationalUnitType.name must not be blank"
        );
        String normalizedDescription = normalizeOptionalText(organizationalUnitType.description());
        if (organizationalUnitType.nodeKind() == null) {
            throw new ValidationException("organizationalUnitType.nodeKind must not be null");
        }

        Long actorUserId = resolveActorUserId();
        checkAdmission(actorUserId, OPERATION_CREATE_UNIT_TYPE, null);

        ensureUnitTypeCodeIsUnique(normalizedCode);

        Instant now = utcClock.now();
        OrganizationalUnitType toSave = new OrganizationalUnitType(
                null,
                normalizedCode,
                normalizedName,
                normalizedDescription,
                organizationalUnitType.nodeKind(),
                organizationalUnitType.canBeOperatorHomeUnit(),
                organizationalUnitType.canBeCampaignTarget(),
                organizationalUnitType.participatesInSubtreeScope(),
                organizationalUnitType.canHaveManagementRelation(),
                organizationalUnitType.canHaveAccessArea(),
                now,
                now
        );

        OrganizationalUnitType savedUnitType = organizationalUnitTypeRepository.saveOrganizationalUnitType(toSave);
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.organizational_unit_type.created"),
                ENTITY_TYPE_ORGANIZATIONAL_UNIT_TYPE,
                savedUnitType.id(),
                null,
                savedUnitType,
                auditContextData(OPERATION_CREATE_UNIT_TYPE, Map.of("code", savedUnitType.code()))
        );
        return savedUnitType;
    }

    @Override
    public OrganizationalUnitType updateOrganizationalUnitType(UpdateOrganizationalUnitTypeCommand command) {
        String normalizedName = normalizeRequiredText(
                command.name(),
                "organizationalUnitType.name must not be blank"
        );
        String normalizedDescription = normalizeOptionalText(command.description());

        Long actorUserId = resolveActorUserId();
        checkAdmission(actorUserId, OPERATION_UPDATE_UNIT_TYPE, command.organizationalUnitTypeId());

        OrganizationalUnitType currentUnitType = organizationalUnitTypeRepository.findOrganizationalUnitTypeById(
                command.organizationalUnitTypeId()
        );

        Instant now = utcClock.now();
        OrganizationalUnitType toSave = buildUpdatedUnitType(currentUnitType, command, normalizedName, normalizedDescription, now);
        organizationalUnitSemanticMutationValidationSupport.ensureUnitTypeMutationAllowed(currentUnitType, toSave, now);

        OrganizationalUnitType savedUnitType = organizationalUnitTypeRepository.saveOrganizationalUnitType(toSave);
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.organizational_unit_type.updated"),
                ENTITY_TYPE_ORGANIZATIONAL_UNIT_TYPE,
                savedUnitType.id(),
                currentUnitType,
                savedUnitType,
                auditContextData(
                        OPERATION_UPDATE_UNIT_TYPE,
                        Map.of(
                                "code", savedUnitType.code(),
                                "semanticMutation", hasSemanticMutation(currentUnitType, savedUnitType)
                        )
                )
        );
        return savedUnitType;
    }

    @Override
    public OrganizationalUnit createOrganizationalUnit(OrganizationalUnit organizationalUnit) {
        requireIdAbsent(organizationalUnit.id(), "organizationalUnit");
        String normalizedName = normalizeRequiredText(
                organizationalUnit.name(),
                "organizationalUnit.name must not be blank"
        );
        String normalizedExternalId = normalizeExternalId(organizationalUnit.externalId());
        requireCreateStatusIsActive(organizationalUnit.status());

        Long actorUserId = resolveActorUserId();
        checkAdmission(actorUserId, OPERATION_CREATE_UNIT, null);

        ensureExternalIdIsUniqueOnCreate(normalizedExternalId);

        OrganizationalUnit parentUnit = resolveParentForPlacement(organizationalUnit.parentId(), null);
        ensureRootSlotIsAvailable(parentUnit, null);
        ensureParentAllowsActivePlacement(parentUnit, organizationalUnit.status());

        OrganizationalUnitType targetType = organizationalUnitTypeRepository.findOrganizationalUnitTypeById(
                organizationalUnit.organizationalUnitTypeId()
        );
        OrganizationalUnitTreePosition targetPosition = organizationalUnitTreePathBuilder.resolvePlacement(
                parentUnit,
                normalizedName
        );
        Instant now = utcClock.now();
        OrganizationalUnit toSave = new OrganizationalUnit(
                null,
                parentUnit == null ? null : parentUnit.id(),
                targetType.id(),
                normalizedName,
                OrganizationalUnitStatus.ACTIVE,
                targetPosition.path(),
                targetPosition.depth(),
                normalizedExternalId,
                now,
                now
        );
        ensureCanonicalPathsAreUnique(List.of(toSave));

        OrganizationalUnit savedUnit = organizationalUnitRepository.saveOrganizationalUnit(toSave);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parentId", parentUnit == null ? null : parentUnit.id());
        details.put("organizationalUnitTypeId", targetType.id());
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.organizational_unit.created"),
                ENTITY_TYPE_ORGANIZATIONAL_UNIT,
                savedUnit.id(),
                null,
                savedUnit,
                auditContextData(OPERATION_CREATE_UNIT, details)
        );
        return savedUnit;
    }

    @Override
    public OrganizationalUnit updateOrganizationalUnit(UpdateOrganizationalUnitCommand command) {
        String normalizedName = normalizeRequiredText(command.name(), "organizationalUnit.name must not be blank");
        String normalizedExternalId = normalizeExternalId(command.externalId());

        Long actorUserId = resolveActorUserId();
        checkAdmission(actorUserId, OPERATION_UPDATE_UNIT, command.organizationalUnitId());

        OrganizationalUnit currentUnit = organizationalUnitRepository.findOrganizationalUnitById(command.organizationalUnitId());
        ensureUnitCanBeEdited(currentUnit);
        ensureExternalIdIsUniqueOnUpdate(normalizedExternalId, currentUnit.id());

        Instant now = utcClock.now();
        OrganizationalUnitType targetUnitType = resolveTargetUnitTypeForUpdate(command.organizationalUnitTypeId(), currentUnit);
        organizationalUnitSemanticMutationValidationSupport.ensureUnitTypeReassignmentAllowed(currentUnit, targetUnitType, now);

        OrganizationalUnit parentUnit = resolveParentForPlacement(currentUnit.parentId(), currentUnit.id());
        OrganizationalUnitTreePosition targetPosition = organizationalUnitTreePathBuilder.resolvePlacement(
                parentUnit,
                normalizedName
        );
        OrganizationalUnit updatedUnit = new OrganizationalUnit(
                currentUnit.id(),
                currentUnit.parentId(),
                targetUnitType.id(),
                normalizedName,
                currentUnit.status(),
                targetPosition.path(),
                targetPosition.depth(),
                normalizedExternalId,
                currentUnit.createdAt(),
                now
        );

        List<OrganizationalUnit> plannedUnits = requiresPathRebuild(currentUnit, updatedUnit)
                ? rebuildUpdatedSubtree(currentUnit, updatedUnit, now)
                : List.of(updatedUnit);
        ensureCanonicalPathsAreUnique(plannedUnits);

        OrganizationalUnit savedUnit = null;
        for (OrganizationalUnit plannedUnit : plannedUnits) {
            OrganizationalUnit persistedUnit = organizationalUnitRepository.saveOrganizationalUnit(plannedUnit);
            if (Objects.equals(persistedUnit.id(), currentUnit.id())) {
                savedUnit = persistedUnit;
            }
        }
        if (savedUnit == null) {
            throw new IllegalStateException(
                    "Organizational unit update did not persist the updated root unit: " + currentUnit.id()
            );
        }

        boolean typeReassigned = !Objects.equals(currentUnit.organizationalUnitTypeId(), savedUnit.organizationalUnitTypeId());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("descriptiveUpdate", !typeReassigned);
        details.put("typeReassigned", typeReassigned);
        details.put("pathRebuilt", plannedUnits.size() > 1 || !Objects.equals(currentUnit.path(), savedUnit.path()));
        details.put("affectedSubtreeSize", plannedUnits.size());
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.organizational_unit.updated"),
                ENTITY_TYPE_ORGANIZATIONAL_UNIT,
                savedUnit.id(),
                currentUnit,
                savedUnit,
                auditContextData(OPERATION_UPDATE_UNIT, details)
        );
        return savedUnit;
    }

    @Override
    public OrganizationalUnit moveOrganizationalUnit(Long organizationalUnitId, Long newParentOrganizationalUnitId) {
        requireIdPresent(organizationalUnitId, "organizationalUnitId");

        Long actorUserId = resolveActorUserId();
        checkAdmission(
                actorUserId,
                OPERATION_MOVE_UNIT,
                organizationalUnitId,
                new CapabilityAdmissionPayload.OrganizationalUnitMutation(
                        organizationalUnitId,
                        null,
                        newParentOrganizationalUnitId,
                        null,
                        null,
                        null
                )
        );

        OrganizationalUnit currentUnit = organizationalUnitRepository.findOrganizationalUnitById(organizationalUnitId);
        ensureUnitCanBeEdited(currentUnit);

        organizationalUnitMoveValidator.ensureMoveTargetSelectionAllowed(currentUnit, newParentOrganizationalUnitId);
        OrganizationalUnit targetParent = resolveParentForPlacement(newParentOrganizationalUnitId, currentUnit.id());
        Map<Long, List<OrganizationalUnit>> childrenByParent = loadChildrenByParent(currentUnit);
        List<OrganizationalUnit> currentSubtree = flattenSubtree(currentUnit, childrenByParent);
        organizationalUnitMoveValidator.ensureMoveDoesNotCreateCycle(currentUnit, targetParent, childrenByParent);
        ensureRootSlotIsAvailable(targetParent, currentUnit.id());
        organizationalUnitStructuralMutationValidationSupport.ensureMoveAllowed(currentSubtree, targetParent);

        Instant now = utcClock.now();
        OrganizationalUnitTreePosition targetPosition = organizationalUnitTreePathBuilder.resolvePlacement(
                targetParent,
                currentUnit.name()
        );
        List<OrganizationalUnit> rebuiltSubtree = organizationalUnitSubtreeRebuilder.rebuildSubtree(
                new OrganizationalUnit(
                        currentUnit.id(),
                        targetParent == null ? null : targetParent.id(),
                        currentUnit.organizationalUnitTypeId(),
                        currentUnit.name(),
                        currentUnit.status(),
                        targetPosition.path(),
                        targetPosition.depth(),
                        currentUnit.externalId(),
                        currentUnit.createdAt(),
                        now
                ),
                childrenByParent,
                now
        );
        ensureCanonicalPathsAreUnique(rebuiltSubtree);

        OrganizationalUnit movedUnit = persistUpdatedSubtree(currentUnit.id(), rebuiltSubtree, "move");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousParentId", currentUnit.parentId());
        details.put("newParentId", targetParent == null ? null : targetParent.id());
        details.put("affectedSubtreeSize", rebuiltSubtree.size());
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.organizational_unit.moved"),
                ENTITY_TYPE_ORGANIZATIONAL_UNIT,
                movedUnit.id(),
                currentUnit,
                movedUnit,
                auditContextData(OPERATION_MOVE_UNIT, details)
        );
        return movedUnit;
    }

    @Override
    public OrganizationalUnit archiveOrganizationalUnit(Long organizationalUnitId) {
        requireIdPresent(organizationalUnitId, "organizationalUnitId");

        Long actorUserId = resolveActorUserId();
        checkAdmission(actorUserId, OPERATION_ARCHIVE_UNIT, organizationalUnitId);

        OrganizationalUnit currentUnit = organizationalUnitRepository.findOrganizationalUnitById(organizationalUnitId);
        Map<Long, List<OrganizationalUnit>> childrenByParent = loadChildrenByParent(currentUnit);
        List<OrganizationalUnit> currentSubtree = flattenSubtree(currentUnit, childrenByParent);

        boolean subtreeHasActiveUnits = currentSubtree.stream()
                .anyMatch(unit -> unit.status() == OrganizationalUnitStatus.ACTIVE);
        boolean subtreeHasArchivedUnits = currentSubtree.stream()
                .anyMatch(unit -> unit.status() == OrganizationalUnitStatus.ARCHIVED);

        if (subtreeHasActiveUnits && subtreeHasArchivedUnits) {
            throw new ConflictException(
                    "Organizational unit subtree has mixed ACTIVE/ARCHIVED state and cannot be archived incrementally: "
                            + organizationalUnitId
            );
        }

        if (!subtreeHasActiveUnits) {
            throw new ConflictException("Organizational unit subtree is already archived: " + organizationalUnitId);
        }

        Instant now = utcClock.now();
        organizationalUnitStructuralMutationValidationSupport.ensureArchiveAllowed(currentSubtree, now);
        List<OrganizationalUnit> archivedSubtree = currentSubtree.stream()
                .filter(unit -> unit.status() == OrganizationalUnitStatus.ACTIVE)
                .map(unit -> new OrganizationalUnit(
                        unit.id(),
                        unit.parentId(),
                        unit.organizationalUnitTypeId(),
                        unit.name(),
                        OrganizationalUnitStatus.ARCHIVED,
                        unit.path(),
                        unit.depth(),
                        unit.externalId(),
                        unit.createdAt(),
                        now
                ))
                .toList();

        OrganizationalUnit archivedRoot = currentUnit.status() == OrganizationalUnitStatus.ACTIVE
                ? persistUpdatedSubtree(currentUnit.id(), archivedSubtree, "archive")
                : currentUnit;
        if (currentUnit.status() != OrganizationalUnitStatus.ACTIVE) {
            for (OrganizationalUnit archivedUnit : archivedSubtree) {
                organizationalUnitRepository.saveOrganizationalUnit(archivedUnit);
            }
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("archivedSubtreeSize", archivedSubtree.size());
        details.put("subtreeSize", currentSubtree.size());
        recordAudit(
                actorUserId,
                new AuditEventType("userorg.organizational_unit.archived"),
                ENTITY_TYPE_ORGANIZATIONAL_UNIT,
                currentUnit.id(),
                currentUnit,
                archivedRoot,
                auditContextData(OPERATION_ARCHIVE_UNIT, details)
        );
        return archivedRoot;
    }

    private void requireIdAbsent(Long id, String aggregateName) {
        if (id != null) {
            throw new ValidationException(aggregateName + " id must be null for create command");
        }
    }

    private void requireIdPresent(Long id, String aggregateName) {
        if (id == null) {
            throw new ValidationException(aggregateName + " must not be null");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new ValidationException(message);
        }
        return normalizedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isBlank() ? null : normalizedValue;
    }

    private void requireCreateStatusIsActive(OrganizationalUnitStatus status) {
        if (status != OrganizationalUnitStatus.ACTIVE) {
            throw new ValidationException("New organizational unit must be created in ACTIVE status");
        }
    }

    private String normalizeExternalId(String externalId) {
        if (externalId == null) {
            return null;
        }

        String normalizedExternalId = externalId.trim();
        if (normalizedExternalId.isBlank()) {
            throw new ValidationException("organizationalUnit.externalId must not be blank when provided");
        }
        return normalizedExternalId;
    }

    private void ensureUnitTypeCodeIsUnique(String code) {
        if (organizationalUnitTypeRepository.existsOrganizationalUnitTypeByCode(code)) {
            throw new ConflictException("Organizational unit type code already exists: " + code);
        }
    }

    private void ensureExternalIdIsUniqueOnCreate(String externalId) {
        if (externalId != null && organizationalUnitRepository.existsOrganizationalUnitByExternalId(externalId)) {
            throw new ConflictException("Organizational unit externalId already exists: " + externalId);
        }
    }

    private void ensureExternalIdIsUniqueOnUpdate(String externalId, Long organizationalUnitId) {
        if (externalId != null
                && organizationalUnitRepository.existsOrganizationalUnitByExternalIdAndIdNot(externalId, organizationalUnitId)) {
            throw new ConflictException("Organizational unit externalId already exists: " + externalId);
        }
    }

    private void ensureUnitCanBeEdited(OrganizationalUnit currentUnit) {
        if (currentUnit.status() != OrganizationalUnitStatus.ACTIVE) {
            throw new ConflictException("Archived organizational unit cannot be edited: " + currentUnit.id());
        }
    }

    private OrganizationalUnitType buildUpdatedUnitType(
            OrganizationalUnitType currentUnitType,
            UpdateOrganizationalUnitTypeCommand command,
            String normalizedName,
            String normalizedDescription,
            Instant now
    ) {
        return new OrganizationalUnitType(
                currentUnitType.id(),
                currentUnitType.code(),
                normalizedName,
                normalizedDescription,
                command.nodeKind() != null ? command.nodeKind() : currentUnitType.nodeKind(),
                command.canBeOperatorHomeUnit() != null
                        ? command.canBeOperatorHomeUnit()
                        : currentUnitType.canBeOperatorHomeUnit(),
                command.canBeCampaignTarget() != null
                        ? command.canBeCampaignTarget()
                        : currentUnitType.canBeCampaignTarget(),
                command.participatesInSubtreeScope() != null
                        ? command.participatesInSubtreeScope()
                        : currentUnitType.participatesInSubtreeScope(),
                command.canHaveManagementRelation() != null
                        ? command.canHaveManagementRelation()
                        : currentUnitType.canHaveManagementRelation(),
                command.canHaveAccessArea() != null
                        ? command.canHaveAccessArea()
                        : currentUnitType.canHaveAccessArea(),
                currentUnitType.createdAt(),
                now
        );
    }

    private OrganizationalUnitType resolveTargetUnitTypeForUpdate(Long requestedUnitTypeId, OrganizationalUnit currentUnit) {
        Long targetUnitTypeId = requestedUnitTypeId == null ? currentUnit.organizationalUnitTypeId() : requestedUnitTypeId;
        if (Objects.equals(targetUnitTypeId, currentUnit.organizationalUnitTypeId())) {
            return organizationalUnitTypeRepository.findOrganizationalUnitTypeById(currentUnit.organizationalUnitTypeId());
        }
        return organizationalUnitTypeRepository.findOrganizationalUnitTypeById(targetUnitTypeId);
    }

    private boolean hasSemanticMutation(
            OrganizationalUnitType currentUnitType,
            OrganizationalUnitType candidateUnitType
    ) {
        return currentUnitType.nodeKind() != candidateUnitType.nodeKind()
                || currentUnitType.canBeOperatorHomeUnit() != candidateUnitType.canBeOperatorHomeUnit()
                || currentUnitType.canBeCampaignTarget() != candidateUnitType.canBeCampaignTarget()
                || currentUnitType.participatesInSubtreeScope() != candidateUnitType.participatesInSubtreeScope()
                || currentUnitType.canHaveManagementRelation() != candidateUnitType.canHaveManagementRelation()
                || currentUnitType.canHaveAccessArea() != candidateUnitType.canHaveAccessArea();
    }

    private OrganizationalUnit resolveParentForPlacement(Long parentUnitId, Long currentUnitId) {
        if (parentUnitId == null) {
            return null;
        }

        return organizationalUnitRepository.findOrganizationalUnitById(parentUnitId);
    }

    private void ensureRootSlotIsAvailable(OrganizationalUnit parentUnit, Long currentUnitId) {
        if (parentUnit != null) {
            return;
        }

        OrganizationalUnit existingRoot = findExistingRoot(currentUnitId);
        if (existingRoot != null) {
            throw new ConflictException("Organizational root already exists: " + existingRoot.id());
        }
    }

    private OrganizationalUnit findExistingRoot(Long currentUnitId) {
        return organizationalUnitRepository.findRootUnits().stream()
                .filter(unit -> !Objects.equals(unit.id(), currentUnitId))
                .findFirst()
                .orElse(null);
    }

    private void ensureParentAllowsActivePlacement(OrganizationalUnit parentUnit, OrganizationalUnitStatus childStatus) {
        if (parentUnit == null || childStatus != OrganizationalUnitStatus.ACTIVE) {
            return;
        }

        if (parentUnit.status() != OrganizationalUnitStatus.ACTIVE) {
            throw new ConflictException("Archived organizational unit cannot be parent for an ACTIVE node: " + parentUnit.id());
        }
    }

    private Map<Long, List<OrganizationalUnit>> loadChildrenByParent(OrganizationalUnit rootUnit) {
        Map<Long, List<OrganizationalUnit>> childrenByParent = new LinkedHashMap<>();
        collectChildren(rootUnit, childrenByParent, new HashSet<>());
        return childrenByParent;
    }

    private void collectChildren(
            OrganizationalUnit parentUnit,
            Map<Long, List<OrganizationalUnit>> childrenByParent,
            Set<Long> visitedIds
    ) {
        if (!visitedIds.add(parentUnit.id())) {
            throw new ConflictException("Persisted organizational structure already contains a cycle at unit: " + parentUnit.id());
        }

        List<OrganizationalUnit> children = organizationalUnitRepository.findChildUnits(parentUnit.id());
        childrenByParent.put(parentUnit.id(), children);

        for (OrganizationalUnit child : children) {
            collectChildren(child, childrenByParent, visitedIds);
        }
    }

    private List<OrganizationalUnit> flattenSubtree(
            OrganizationalUnit rootUnit,
            Map<Long, List<OrganizationalUnit>> childrenByParent
    ) {
        List<OrganizationalUnit> subtree = new ArrayList<>();
        flattenSubtree(rootUnit, childrenByParent, subtree);
        return subtree;
    }

    private void flattenSubtree(
            OrganizationalUnit currentUnit,
            Map<Long, List<OrganizationalUnit>> childrenByParent,
            List<OrganizationalUnit> subtree
    ) {
        subtree.add(currentUnit);
        for (OrganizationalUnit child : childrenByParent.getOrDefault(currentUnit.id(), List.of())) {
            flattenSubtree(child, childrenByParent, subtree);
        }
    }

    private boolean requiresPathRebuild(OrganizationalUnit currentUnit, OrganizationalUnit updatedUnit) {
        return !Objects.equals(currentUnit.path(), updatedUnit.path())
                || currentUnit.depth() != updatedUnit.depth();
    }

    private List<OrganizationalUnit> rebuildUpdatedSubtree(
            OrganizationalUnit currentUnit,
            OrganizationalUnit updatedUnit,
            Instant updatedAt
    ) {
        Map<Long, List<OrganizationalUnit>> childrenByParent = loadChildrenByParent(currentUnit);
        return organizationalUnitSubtreeRebuilder.rebuildSubtree(updatedUnit, childrenByParent, updatedAt);
    }

    private void ensureCanonicalPathsAreUnique(List<OrganizationalUnit> units) {
        Set<String> plannedPaths = new HashSet<>();

        for (OrganizationalUnit unit : units) {
            if (!plannedPaths.add(unit.path())) {
                throw new ConflictException(
                        "Canonical organizationalUnit.path is duplicated inside the planned subtree: " + unit.path()
                );
            }

            boolean pathConflict = unit.id() == null
                    ? organizationalUnitRepository.existsOrganizationalUnitByPath(unit.path())
                    : organizationalUnitRepository.existsOrganizationalUnitByPathAndIdNot(unit.path(), unit.id());
            if (pathConflict) {
                throw new ConflictException("Canonical organizationalUnit.path already exists: " + unit.path());
            }
        }
    }

    private OrganizationalUnit persistUpdatedSubtree(
            Long rootUnitId,
            List<OrganizationalUnit> plannedUnits,
            String operationLabel
    ) {
        OrganizationalUnit persistedRoot = null;
        for (OrganizationalUnit plannedUnit : plannedUnits) {
            OrganizationalUnit persistedUnit = organizationalUnitRepository.saveOrganizationalUnit(plannedUnit);
            if (Objects.equals(persistedUnit.id(), rootUnitId)) {
                persistedRoot = persistedUnit;
            }
        }
        if (persistedRoot == null) {
            throw new IllegalStateException(
                    "Organizational unit " + operationLabel + " did not persist the updated root unit: " + rootUnitId
            );
        }
        return persistedRoot;
    }
    private void checkAdmission(Long actorUserId, String operationCode, Long targetEntityId) {
        checkAdmission(actorUserId, operationCode, targetEntityId, CapabilityAdmissionPayload.Empty.INSTANCE);
    }

    private void checkAdmission(
            Long actorUserId,
            String operationCode,
            Long targetEntityId,
            CapabilityAdmissionPayload payloadContext
    ) {
        capabilityAdmissionPolicy.check(
                capabilityAdmissionRequestFactory.create(
                        actorUserId,
                        operationCode,
                        resolveTargetEntityType(operationCode),
                        targetEntityId,
                        payloadContext
                )
        );
    }

    private CapabilityTargetEntityType resolveTargetEntityType(String operationCode) {
        return switch (operationCode) {
            case OPERATION_CREATE_UNIT_TYPE, OPERATION_UPDATE_UNIT_TYPE -> CapabilityTargetEntityType.ORGANIZATIONAL_UNIT_TYPE;
            case OPERATION_CREATE_UNIT, OPERATION_UPDATE_UNIT, OPERATION_MOVE_UNIT, OPERATION_ARCHIVE_UNIT ->
                    CapabilityTargetEntityType.ORGANIZATIONAL_UNIT;
            default -> CapabilityTargetEntityType.ORGANIZATIONAL_UNIT;
        };
    }
    private Long resolveActorUserId() {
        return criticalCommandAuditSupport.resolveInteractiveActorUserId();
    }

    private void recordAudit(
            Long actorUserId,
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Object payloadBefore,
            Object payloadAfter,
            AuditContext contextPayload
    ) {
        criticalCommandAuditSupport.recordAudit(
                actorUserId,
                eventType,
                entityType,
                entityId,
                payloadBefore,
                payloadAfter,
                contextPayload
        );
    }

    private AuditContext auditContextData(String operationCode, Map<String, Object> details) {
        return criticalCommandAuditSupport.buildAuditContext(TARGET_MODULE, operationCode, details);
    }
}














