package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Класс {@code OrganizationalUnitSubtreeRebuilder}.
 */
@Component
final class OrganizationalUnitSubtreeRebuilder {

    private final OrganizationalUnitTreePathBuilder organizationalUnitTreePathBuilder;

    OrganizationalUnitSubtreeRebuilder(OrganizationalUnitTreePathBuilder organizationalUnitTreePathBuilder) {
        this.organizationalUnitTreePathBuilder = organizationalUnitTreePathBuilder;
    }

    List<OrganizationalUnit> rebuildSubtree(
        OrganizationalUnit rootUnit,
        Map<Long, List<OrganizationalUnit>> childrenByParent,
        Instant updatedAt
    ) {
        List<OrganizationalUnit> rebuiltUnits = new ArrayList<>();
        rebuildSubtree(rootUnit, childrenByParent, updatedAt, rebuiltUnits);
        return rebuiltUnits;
    }

    private void rebuildSubtree(
        OrganizationalUnit parentUnit,
        Map<Long, List<OrganizationalUnit>> childrenByParent,
        Instant updatedAt,
        List<OrganizationalUnit> rebuiltUnits
    ) {
        rebuiltUnits.add(parentUnit);

        for (OrganizationalUnit child : childrenByParent.getOrDefault(parentUnit.id(), List.of())) {
            OrganizationalUnitTreePosition childPosition = organizationalUnitTreePathBuilder.resolvePlacement(
                parentUnit,
                child.name()
            );
            OrganizationalUnit rebuiltChild = new OrganizationalUnit(
                child.id(),
                parentUnit.id(),
                child.organizationalUnitTypeId(),
                child.name(),
                child.status(),
                childPosition.path(),
                childPosition.depth(),
                child.externalId(),
                child.createdAt(),
                updatedAt
            );
            rebuildSubtree(rebuiltChild, childrenByParent, updatedAt, rebuiltUnits);
        }
    }
}