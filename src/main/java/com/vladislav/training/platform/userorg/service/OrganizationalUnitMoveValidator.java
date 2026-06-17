package com.vladislav.training.platform.userorg.service;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Проверка {@code OrganizationalUnitMoveValidator}.
 */
@Component
final class OrganizationalUnitMoveValidator {

    void ensureMoveTargetSelectionAllowed(OrganizationalUnit currentUnit, Long requestedParentId) {
        if (Objects.equals(currentUnit.id(), requestedParentId)) {
            throw new ValidationException("organizationalUnit.parentId must not reference the same unit");
        }
        if (Objects.equals(currentUnit.parentId(), requestedParentId)) {
            throw new ValidationException("Move target must differ from the current organizationalUnit.parentId");
        }
    }

    void ensureMoveDoesNotCreateCycle(
            OrganizationalUnit currentUnit,
            OrganizationalUnit targetParent,
            Map<Long, List<OrganizationalUnit>> childrenByParent
    ) {
        if (targetParent == null) {
            return;
        }

        if (Objects.equals(currentUnit.id(), targetParent.id())) {
            throw new ValidationException("organizationalUnit.parentId must not reference the same unit");
        }

        if (isInsideCurrentSubtree(currentUnit.id(), targetParent.id(), childrenByParent)) {
            throw new ConflictException("Moving organizational unit would create a cycle: " + currentUnit.id());
        }
    }

    private boolean isInsideCurrentSubtree(
            Long currentUnitId,
            Long candidateUnitId,
            Map<Long, List<OrganizationalUnit>> childrenByParent
    ) {
        if (Objects.equals(currentUnitId, candidateUnitId)) {
            return true;
        }

        for (OrganizationalUnit child : childrenByParent.getOrDefault(currentUnitId, List.of())) {
            if (isInsideCurrentSubtree(child.id(), candidateUnitId, childrenByParent)) {
                return true;
            }
        }
        return false;
    }
}
