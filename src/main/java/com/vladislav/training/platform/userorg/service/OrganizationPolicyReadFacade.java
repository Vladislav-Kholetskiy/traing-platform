package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Класс {@code OrganizationPolicyReadFacade}.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationPolicyReadFacade {

    private final OrganizationPolicyReadService policyReadService;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;
    private final OrganizationQueryService organizationQueryService;

    public OrganizationPolicyReadFacade(
            OrganizationPolicyReadService policyReadService,
            AccessSpecificationPolicy accessSpecificationPolicy,
            AccessPolicyQueryContextResolver queryContextResolver,
            OrganizationQueryService organizationQueryService
    ) {
        this.policyReadService = policyReadService;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.queryContextResolver = queryContextResolver;
        this.organizationQueryService = organizationQueryService;
    }

    public OrganizationalUnit findOrganizationalUnitById(Long organizationalUnitId) {
        AccessReadScope scope = ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.ORGANIZATION,
                AccessReadType.DETAIL,
                null,
                organizationalUnitId,
                "organizational_unit"
        ));
        return policyReadService.findOrganizationalUnitByIdWithinScope(scope, organizationalUnitId)
                .orElseThrow(() -> new NotFoundException("Organizational unit not found by id: " + organizationalUnitId));
    }

    public OrganizationalUnit findOrganizationalUnitByPath(String path) {
        AccessReadScope scope = ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.ORGANIZATION,
                AccessReadType.DETAIL,
                null,
                null,
                "organizational_unit"
        ));
        return policyReadService.findOrganizationalUnitByPathWithinScope(scope, path)
                .orElseThrow(() -> new NotFoundException("Organizational unit not found by path: " + path));
    }

    public List<OrganizationalUnit> findChildUnits(Long parentUnitId) {
        AccessReadScope scope = ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.ORGANIZATION,
                AccessReadType.LIST,
                null,
                parentUnitId,
                "organizational_unit"
        ));
        return policyReadService.findChildUnitsWithinScope(scope, parentUnitId);
    }

    public List<OrganizationalUnit> findOrganizationalUnits(OrganizationalUnitStatus status) {
        AccessReadScope scope = ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.ORGANIZATION,
                AccessReadType.LIST,
                null,
                null,
                "organizational_unit"
        ));
        return policyReadService.findUnitsWithinScope(scope, status);
    }

    public List<OrganizationalUnit> findOrganizationalUnitTree(OrganizationalUnitStatus status) {
        AccessReadScope scope = ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.ORGANIZATION,
                AccessReadType.TREE,
                null,
                null,
                "organizational_unit"
        ));
        return policyReadService.findUnitsWithinScope(scope, status);
    }

    public Map<Long, OrganizationalUnit> findOrganizationalUnitsByIdsWithinScope(
            AccessReadScope scope,
            Collection<Long> organizationalUnitIds
    ) {
        if (!scope.readAllowed() || organizationalUnitIds == null || organizationalUnitIds.isEmpty()) {
            return Map.of();
        }
        return policyReadService.findOrganizationalUnitsByIdsWithinScope(scope, organizationalUnitIds).stream()
                .collect(Collectors.toMap(
                        OrganizationalUnit::id,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public OrganizationalUnitType findOrganizationalUnitTypeById(Long organizationalUnitTypeId) {
        ensureReferenceReadAllowed();
        return organizationQueryService.findOrganizationalUnitTypeById(organizationalUnitTypeId);
    }

    public OrganizationalUnitType findOrganizationalUnitTypeByCode(String organizationalUnitTypeCode) {
        ensureReferenceReadAllowed();
        return organizationQueryService.findOrganizationalUnitTypeByCode(organizationalUnitTypeCode);
    }

    public List<OrganizationalUnitType> findUnitTypesByNodeKind(OrganizationalNodeKind nodeKind) {
        ensureReferenceReadAllowed();
        return organizationQueryService.findUnitTypesByNodeKind(nodeKind);
    }

    public List<OrganizationalUnitType> findAllOrganizationalUnitTypes() {
        ensureReferenceReadAllowed();
        Map<Long, OrganizationalUnitType> unitTypesById = new TreeMap<>();
        for (OrganizationalNodeKind nodeKind : OrganizationalNodeKind.values()) {
            for (OrganizationalUnitType unitType : organizationQueryService.findUnitTypesByNodeKind(nodeKind)) {
                unitTypesById.putIfAbsent(unitType.id(), unitType);
            }
        }
        return List.copyOf(unitTypesById.values());
    }

    private AccessReadScope ensureReadAllowed(AccessPolicyQueryContext context) {
        AccessReadScope scope = accessSpecificationPolicy.resolveReadScope(context);
        if (!scope.readAllowed()) {
            throw new PolicyViolationException("Organizational unit read is forbidden by AccessSpecificationPolicy");
        }
        return scope;
    }

    private void ensureReferenceReadAllowed() {
        ensureReadAllowed(queryContextResolver.resolve(
                AccessReadArea.ORGANIZATION,
                AccessReadType.REFERENCE_READ,
                "organizational_unit_type"
        ));
    }
}