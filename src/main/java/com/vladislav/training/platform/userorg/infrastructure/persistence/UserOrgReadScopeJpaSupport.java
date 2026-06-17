package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class UserOrgReadScopeJpaSupport {

    private UserOrgReadScopeJpaSupport() {
    }

    public static Specification<OrganizationalUnitEntity> organizationalUnitWithinScope(AccessReadScope scope) {
        return (root, query, criteriaBuilder) -> organizationalUnitPredicate(
            scope,
            root.get("id"),
            root.get("path"),
            criteriaBuilder
        );
    }

    public static <T> Specification<T> organizationalUnitFieldWithinScope(AccessReadScope scope, String organizationalUnitIdField) {
        return (root, query, criteriaBuilder) -> {
            if (!scope.readAllowed()) {
                return criteriaBuilder.disjunction();
            }
            if (scope.fullOrganizationalUnitAccess()) {
                return criteriaBuilder.conjunction();
            }

            var subquery = query.subquery(Long.class);
            var unitRoot = subquery.from(OrganizationalUnitEntity.class);
            subquery.select(unitRoot.get("id"))
                .where(organizationalUnitPredicate(scope, unitRoot.get("id"), unitRoot.get("path"), criteriaBuilder));
            return root.get(organizationalUnitIdField).in(subquery);
        };
    }

    public static <T> Specification<T> currentUserVisibleWithinScope(
        AccessReadScope scope,
        Instant activeAt,
        String userIdField
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!scope.readAllowed()) {
                return criteriaBuilder.disjunction();
            }
            if (scope.fullOrganizationalUnitAccess()) {
                return criteriaBuilder.conjunction();
            }

            var subquery = query.subquery(Long.class);
            var assignmentRoot = subquery.from(UserOrganizationAssignmentEntity.class);
            var unitRoot = subquery.from(OrganizationalUnitEntity.class);
            subquery.select(assignmentRoot.get("userId"))
                .where(
                    criteriaBuilder.equal(assignmentRoot.get("userId"), root.get(userIdField)),
                    criteriaBuilder.equal(assignmentRoot.get("organizationalUnitId"), unitRoot.get("id")),
                    criteriaBuilder.lessThanOrEqualTo(assignmentRoot.get("validFrom"), activeAt),
                    criteriaBuilder.or(
                        criteriaBuilder.isNull(assignmentRoot.get("validTo")),
                        criteriaBuilder.greaterThan(assignmentRoot.get("validTo"), activeAt)
                    ),
                    organizationalUnitPredicate(scope, unitRoot.get("id"), unitRoot.get("path"), criteriaBuilder)
                );
            return criteriaBuilder.exists(subquery);
        };
    }

    private static jakarta.persistence.criteria.Predicate organizationalUnitPredicate(
        AccessReadScope scope,
        jakarta.persistence.criteria.Path<Long> idPath,
        jakarta.persistence.criteria.Path<String> pathPath,
        jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
    ) {
        if (!scope.readAllowed()) {
            return criteriaBuilder.disjunction();
        }
        if (scope.fullOrganizationalUnitAccess()) {
            return criteriaBuilder.conjunction();
        }

        List<jakarta.persistence.criteria.Predicate> allowedPredicates = new ArrayList<>();
        if (!scope.unitOnlyIds().isEmpty()) {
            allowedPredicates.add(idPath.in(scope.unitOnlyIds()));
        }
        for (String subtreePath : scope.subtreePaths()) {
            allowedPredicates.add(criteriaBuilder.equal(pathPath, subtreePath));
            allowedPredicates.add(criteriaBuilder.like(pathPath, subtreePath + "/%"));
        }

        if (allowedPredicates.isEmpty()) {
            return criteriaBuilder.disjunction();
        }
        return criteriaBuilder.or(allowedPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    }
}
