package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/**
 * Вспомогательный тип {@code AccessReadScopeRestrictionSupport}.
 */
public final class AccessReadScopeRestrictionSupport {

    private AccessReadScopeRestrictionSupport() {
    }

    public static <T> Specification<T> organizationalUnitFieldWithinScope(
        AccessReadScope scope,
        Set<Long> organizationalUnitIds,
        String organizationalUnitIdField
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!scope.readAllowed()) {
                return criteriaBuilder.disjunction();
            }
            if (scope.fullOrganizationalUnitAccess()) {
                return criteriaBuilder.conjunction();
            }
            if (organizationalUnitIds.isEmpty()) {
                return criteriaBuilder.disjunction();
            }
            return root.get(organizationalUnitIdField).in(organizationalUnitIds);
        };
    }

    public static <T> Specification<T> currentUserVisibleWithinScope(
        AccessReadScope scope,
        Set<Long> visibleUserIds,
        String userIdField
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!scope.readAllowed()) {
                return criteriaBuilder.disjunction();
            }
            if (scope.fullOrganizationalUnitAccess()) {
                return criteriaBuilder.conjunction();
            }
            if (visibleUserIds.isEmpty()) {
                return criteriaBuilder.disjunction();
            }
            return root.get(userIdField).in(visibleUserIds);
        };
    }

    public static <T> Specification<T> accessAreaFieldWithinScope(
        AccessReadScope scope,
        Set<Long> organizationalUnitIds,
        Set<Long> visibleUserIds,
        String organizationalUnitIdField,
        String userIdField
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!scope.readAllowed()) {
                return criteriaBuilder.disjunction();
            }
            if (scope.fullOrganizationalUnitAccess()) {
                return criteriaBuilder.conjunction();
            }

            var directPredicate = organizationalUnitIds.isEmpty()
                ? criteriaBuilder.disjunction()
                : root.get(organizationalUnitIdField).in(organizationalUnitIds);
            var globalVisibilityPredicate = visibleUserIds.isEmpty()
                ? criteriaBuilder.disjunction()
                : criteriaBuilder.and(
                    criteriaBuilder.isNull(root.get(organizationalUnitIdField)),
                    root.get(userIdField).in(visibleUserIds)
                );
            return criteriaBuilder.or(directPredicate, globalVisibilityPredicate);
        };
    }
}
