package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.userorg.domain.OrganizationAssignmentType;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.domain.UserOrganizationAssignment;
import com.vladislav.training.platform.userorg.service.OrganizationQueryService;
import com.vladislav.training.platform.userorg.service.UserOrganizationAssignmentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AssignmentNotificationAudienceResolver {

    private static final String PRODUCTION_BLOCK_UNIT_TYPE_CODE = "production_block";

    private final OrganizationQueryService organizationQueryService;
    private final UserOrganizationAssignmentService userOrganizationAssignmentService;
    private final ManagementRelationRepository managementRelationRepository;
    private final TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;

    AssignmentNotificationAudienceResolver(
        OrganizationQueryService organizationQueryService,
        UserOrganizationAssignmentService userOrganizationAssignmentService,
        ManagementRelationRepository managementRelationRepository,
        TemporaryManagementDelegationRepository temporaryManagementDelegationRepository
    ) {
        this.organizationQueryService = Objects.requireNonNull(
            organizationQueryService,
            "organizationQueryService must not be null"
        );
        this.userOrganizationAssignmentService = Objects.requireNonNull(
            userOrganizationAssignmentService,
            "userOrganizationAssignmentService must not be null"
        );
        this.managementRelationRepository = Objects.requireNonNull(
            managementRelationRepository,
            "managementRelationRepository must not be null"
        );
        this.temporaryManagementDelegationRepository = Objects.requireNonNull(
            temporaryManagementDelegationRepository,
            "temporaryManagementDelegationRepository must not be null"
        );
    }

    Set<Long> resolveManagerUserIdsForUnit(Long organizationalUnitId, Instant activeAt) {
        return resolveManagerUserIdsForUnit(
            organizationalUnitId,
            activeAt,
            new LinkedHashMap<>(),
            new LinkedHashMap<>(),
            new LinkedHashMap<>()
        );
    }

    Set<Long> resolveManagerUserIdsForUser(Long userId, Instant activeAt) {
        List<UserOrganizationAssignment> activeAssignments =
            userOrganizationAssignmentService.findActiveOrganizationAssignmentsByUserId(userId, activeAt).stream()
                .filter(assignment -> assignment.assignmentType() == OrganizationAssignmentType.PRIMARY)
                .toList();
        if (activeAssignments.size() != 1) {
            return Set.of();
        }
        return resolveManagerUserIdsForUnit(activeAssignments.get(0).organizationalUnitId(), activeAt);
    }

    private Set<Long> resolveManagerUserIdsForUnit(
        Long organizationalUnitId,
        Instant activeAt,
        Map<Long, OrganizationalUnit> unitCache,
        Map<Long, String> unitTypeCodeCache,
        Map<Long, Set<Long>> managerCache
    ) {
        if (managerCache.containsKey(organizationalUnitId)) {
            return managerCache.get(organizationalUnitId);
        }

        LinkedHashSet<Long> managerUserIds = new LinkedHashSet<>();
        OrganizationalUnit cursor = getUnit(organizationalUnitId, unitCache);
        while (cursor != null) {
            managerUserIds.addAll(findActiveManagerUserIds(cursor.id(), activeAt));

            String unitTypeCode = getUnitTypeCode(cursor.organizationalUnitTypeId(), unitTypeCodeCache);
            if (PRODUCTION_BLOCK_UNIT_TYPE_CODE.equals(unitTypeCode) || cursor.parentId() == null) {
                break;
            }
            cursor = getUnit(cursor.parentId(), unitCache);
        }

        Set<Long> cached = Collections.unmodifiableSet(new LinkedHashSet<>(managerUserIds));
        managerCache.put(organizationalUnitId, cached);
        return cached;
    }

    private OrganizationalUnit getUnit(Long organizationalUnitId, Map<Long, OrganizationalUnit> unitCache) {
        return unitCache.computeIfAbsent(organizationalUnitId, organizationQueryService::findOrganizationalUnitById);
    }

    private String getUnitTypeCode(Long organizationalUnitTypeId, Map<Long, String> unitTypeCodeCache) {
        return unitTypeCodeCache.computeIfAbsent(organizationalUnitTypeId, unitTypeId -> {
            OrganizationalUnitType unitType = organizationQueryService.findOrganizationalUnitTypeById(unitTypeId);
            return unitType.code();
        });
    }

    private List<Long> findActiveManagerUserIds(Long organizationalUnitId, Instant activeAt) {
        LinkedHashSet<Long> managerUserIds = new LinkedHashSet<>();

        for (ManagementRelation relation :
            managementRelationRepository.findManagementRelationsByOrganizationalUnitId(organizationalUnitId)) {
            if (isActiveAt(relation.validFrom(), relation.validTo(), activeAt)) {
                managerUserIds.add(relation.userId());
            }
        }

        for (TemporaryManagementDelegation delegation :
            temporaryManagementDelegationRepository.findTemporaryManagementDelegationsByOrganizationalUnitId(
                organizationalUnitId
            )) {
            if (isActiveAt(delegation.validFrom(), delegation.validTo(), activeAt)) {
                managerUserIds.add(delegation.userId());
            }
        }

        return new ArrayList<>(managerUserIds);
    }

    private boolean isActiveAt(Instant validFrom, Instant validTo, Instant activeAt) {
        return !validFrom.isAfter(activeAt) && (validTo == null || validTo.isAfter(activeAt));
    }
}
