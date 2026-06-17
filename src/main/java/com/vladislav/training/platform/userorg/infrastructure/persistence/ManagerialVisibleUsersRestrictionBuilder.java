package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagerialReadScope;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

public final class ManagerialVisibleUsersRestrictionBuilder {

    private ManagerialVisibleUsersRestrictionBuilder() {
    }

    public static ManagerialVisibleUsersRestriction build(ManagerialReadScope managerialReadScope) {
        Objects.requireNonNull(managerialReadScope, "managerialReadScope must not be null");

        AccessReadScope readScope = managerialReadScope.readScope();
        if (!readScope.readAllowed()) {
            return ManagerialVisibleUsersRestriction.denyAll(managerialReadScope.effectiveAt());
        }
        if (readScope.fullOrganizationalUnitAccess()) {
            return new ManagerialVisibleUsersRestriction(managerialReadScope.effectiveAt(), readScope);
        }
        if (readScope.unitOnlyIds().isEmpty() && readScope.subtreePaths().isEmpty()) {
            return ManagerialVisibleUsersRestriction.denyAll(managerialReadScope.effectiveAt());
        }
        return new ManagerialVisibleUsersRestriction(managerialReadScope.effectiveAt(), readScope);
    }

    public record ManagerialVisibleUsersRestriction(
        Instant effectiveAt,
        AccessReadScope readScope
    ) {

        public ManagerialVisibleUsersRestriction {
            Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
            Objects.requireNonNull(readScope, "readScope must not be null");
        }

        public static ManagerialVisibleUsersRestriction denyAll(Instant effectiveAt) {
            return new ManagerialVisibleUsersRestriction(effectiveAt, AccessReadScope.denyAll());
        }

        public boolean failClosed() {
            return !readScope.readAllowed();
        }

        public boolean unitRestricted() {
            return !readScope.unitOnlyIds().isEmpty();
        }

        public boolean subtreeRestricted() {
            return !readScope.subtreePaths().isEmpty();
        }

        public boolean fullAccess() {
            return readScope.fullOrganizationalUnitAccess();
        }

        public <T> Specification<T> toSpecification(String userIdField) {
            Objects.requireNonNull(userIdField, "userIdField must not be null");
            return UserOrgReadScopeJpaSupport.currentUserVisibleWithinScope(readScope, effectiveAt, userIdField);
        }
    }
}
