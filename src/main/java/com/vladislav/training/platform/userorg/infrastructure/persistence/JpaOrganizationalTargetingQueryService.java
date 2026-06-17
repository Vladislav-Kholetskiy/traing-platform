package com.vladislav.training.platform.userorg.infrastructure.persistence;

import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса чтения {@code JpaOrganizationalTargetingQueryService}.
 */
@Service
@Transactional(readOnly = true)
public class JpaOrganizationalTargetingQueryService implements OrganizationalTargetingQueryService {

    private final SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    private final SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;

    public JpaOrganizationalTargetingQueryService(
        SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository,
        SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository
    ) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.userOrganizationAssignmentRepository = userOrganizationAssignmentRepository;
    }

    @Override
    public Set<Long> resolveCurrentCandidateUserIdsForUnitSubtree(String organizationalUnitPath, Instant effectiveAt) {
        List<Long> organizationalUnitIds = organizationalUnitRepository.findAllInSubtreeByPath(organizationalUnitPath).stream()
            .map(OrganizationalUnitEntity::getId)
            .toList();
        if (organizationalUnitIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(userOrganizationAssignmentRepository.findDistinctActiveUserIdsByOrganizationalUnitIdIn(
            organizationalUnitIds,
            effectiveAt
        ));
    }
}
