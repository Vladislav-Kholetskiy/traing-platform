package com.vladislav.training.platform.access.infrastructure.persistence;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.access.service.AccessAdministrationPolicyReadService;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.ManagementRelationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryAccessAreaAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryManagementDelegationAdminFilter;
import com.vladislav.training.platform.access.service.TemporaryRoleAssignmentAdminFilter;
import com.vladislav.training.platform.access.service.UserAccessAreaAdminFilter;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code JpaAccessAdministrationPolicyReadService}.
 */
@Service
@Transactional(readOnly = true)
public class JpaAccessAdministrationPolicyReadService implements AccessAdministrationPolicyReadService {

    private final PolicyScopedUserAccessAreaReadRepository userAccessAreaReadRepository;
    private final PolicyScopedManagementRelationReadRepository managementRelationReadRepository;
    private final PolicyScopedTemporaryRoleAssignmentReadRepository temporaryRoleAssignmentReadRepository;
    private final PolicyScopedTemporaryAccessAreaReadRepository temporaryAccessAreaReadRepository;
    private final PolicyScopedTemporaryManagementDelegationReadRepository temporaryManagementDelegationReadRepository;

    public JpaAccessAdministrationPolicyReadService(
        PolicyScopedUserAccessAreaReadRepository userAccessAreaReadRepository,
        PolicyScopedManagementRelationReadRepository managementRelationReadRepository,
        PolicyScopedTemporaryRoleAssignmentReadRepository temporaryRoleAssignmentReadRepository,
        PolicyScopedTemporaryAccessAreaReadRepository temporaryAccessAreaReadRepository,
        PolicyScopedTemporaryManagementDelegationReadRepository temporaryManagementDelegationReadRepository
    ) {
        this.userAccessAreaReadRepository = userAccessAreaReadRepository;
        this.managementRelationReadRepository = managementRelationReadRepository;
        this.temporaryRoleAssignmentReadRepository = temporaryRoleAssignmentReadRepository;
        this.temporaryAccessAreaReadRepository = temporaryAccessAreaReadRepository;
        this.temporaryManagementDelegationReadRepository = temporaryManagementDelegationReadRepository;
    }

    @Override
    public List<UserAccessArea> findUserAccessAreasWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        UserAccessAreaAdminFilter filter
    ) {
        return userAccessAreaReadRepository.findUserAccessAreasWithinScope(scope, effectiveAt, filter);
    }

    @Override
    public List<ManagementRelation> findManagementRelationsWithinScope(
        AccessReadScope scope,
        ManagementRelationAdminFilter filter
    ) {
        return managementRelationReadRepository.findManagementRelationsWithinScope(scope, filter);
    }

    @Override
    public List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        TemporaryRoleAssignmentAdminFilter filter
    ) {
        return temporaryRoleAssignmentReadRepository.findTemporaryRoleAssignmentsWithinScope(scope, effectiveAt, filter);
    }

    @Override
    public List<TemporaryAccessArea> findTemporaryAccessAreasWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        TemporaryAccessAreaAdminFilter filter
    ) {
        return temporaryAccessAreaReadRepository.findTemporaryAccessAreasWithinScope(scope, effectiveAt, filter);
    }

    @Override
    public List<TemporaryManagementDelegation> findTemporaryManagementDelegationsWithinScope(
        AccessReadScope scope,
        TemporaryManagementDelegationAdminFilter filter
    ) {
        return temporaryManagementDelegationReadRepository.findTemporaryManagementDelegationsWithinScope(scope, filter);
    }
}
