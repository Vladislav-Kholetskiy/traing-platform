package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.service.OrganizationPolicyReadService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code JpaOrganizationPolicyReadService}.
 */
@Service
@Transactional(readOnly = true)
public class JpaOrganizationPolicyReadService implements OrganizationPolicyReadService {

    private final PolicyScopedOrganizationalUnitReadRepository organizationalUnitReadRepository;

    public JpaOrganizationPolicyReadService(PolicyScopedOrganizationalUnitReadRepository organizationalUnitReadRepository) {
        this.organizationalUnitReadRepository = organizationalUnitReadRepository;
    }

    @Override
    public Optional<OrganizationalUnit> findOrganizationalUnitByIdWithinScope(AccessReadScope scope, Long organizationalUnitId) {
        return organizationalUnitReadRepository.findOrganizationalUnitByIdWithinScope(scope, organizationalUnitId);
    }

    @Override
    public Optional<OrganizationalUnit> findOrganizationalUnitByPathWithinScope(AccessReadScope scope, String path) {
        return organizationalUnitReadRepository.findOrganizationalUnitByPathWithinScope(scope, path);
    }

    @Override
    public List<OrganizationalUnit> findChildUnitsWithinScope(AccessReadScope scope, Long parentUnitId) {
        return organizationalUnitReadRepository.findChildUnitsWithinScope(scope, parentUnitId);
    }

    @Override
    public List<OrganizationalUnit> findUnitsWithinScope(AccessReadScope scope, OrganizationalUnitStatus status) {
        return organizationalUnitReadRepository.findUnitsWithinScope(scope, status);
    }

    @Override
    public List<OrganizationalUnit> findOrganizationalUnitsByIdsWithinScope(
            AccessReadScope scope,
            Collection<Long> organizationalUnitIds
    ) {
        return organizationalUnitReadRepository.findOrganizationalUnitsByIdsWithinScope(scope, organizationalUnitIds);
    }
}