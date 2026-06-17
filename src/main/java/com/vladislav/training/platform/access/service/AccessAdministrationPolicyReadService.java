package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import java.time.Instant;
import java.util.List;

/**
 * Контракт сервиса {@code AccessAdministrationPolicyReadService}.
 */
public interface AccessAdministrationPolicyReadService {

    List<UserAccessArea> findUserAccessAreasWithinScope(AccessReadScope scope, Instant effectiveAt, UserAccessAreaAdminFilter filter);

    List<ManagementRelation> findManagementRelationsWithinScope(AccessReadScope scope, ManagementRelationAdminFilter filter);

    List<TemporaryRoleAssignment> findTemporaryRoleAssignmentsWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        TemporaryRoleAssignmentAdminFilter filter
    );

    List<TemporaryAccessArea> findTemporaryAccessAreasWithinScope(
        AccessReadScope scope,
        Instant effectiveAt,
        TemporaryAccessAreaAdminFilter filter
    );

    List<TemporaryManagementDelegation> findTemporaryManagementDelegationsWithinScope(
        AccessReadScope scope,
        TemporaryManagementDelegationAdminFilter filter
    );
}
