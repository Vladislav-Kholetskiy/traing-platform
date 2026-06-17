package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.application.query.AccessScopeProjectionService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code JpaAccessScopeProjectionService}.
 */
@Service
@Transactional(readOnly = true)
public class JpaAccessScopeProjectionService implements AccessScopeProjectionService {

    private static final Sort ID_SORT = Sort.by("id");

    private final SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    private final SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;

    public JpaAccessScopeProjectionService(
        SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository,
        SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository
    ) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
    }

    @Override
    public Set<Long> resolveOrganizationalUnitIds(AccessReadScope scope) {
        if (!scope.readAllowed() || scope.fullOrganizationalUnitAccess()) {
            return Set.of();
        }
        Set<Long> organizationalUnitIds = new LinkedHashSet<>();
        for (OrganizationalUnitEntity unit : organizationalUnitRepository.findAll(
            UserOrgReadScopeJpaSupport.organizationalUnitWithinScope(scope),
            ID_SORT
        )) {
            organizationalUnitIds.add(unit.getId());
        }
        return Set.copyOf(organizationalUnitIds);
    }

    @Override
    public Set<Long> resolveVisibleUserIds(AccessReadScope scope, Instant effectiveAt) {
        if (!scope.readAllowed() || scope.fullOrganizationalUnitAccess()) {
            return Set.of();
        }
        Set<Long> organizationalUnitIds = resolveOrganizationalUnitIds(scope);
        if (organizationalUnitIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(userOrganizationAssignmentRepository.findDistinctActiveUserIdsByOrganizationalUnitIdIn(
            organizationalUnitIds,
            effectiveAt
        ));
    }
}
