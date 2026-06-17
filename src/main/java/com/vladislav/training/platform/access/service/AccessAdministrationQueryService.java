package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import java.util.List;

/**
 * Контракт сервиса чтения {@code AccessAdministrationQueryService}.
 */
public interface AccessAdministrationQueryService {

    List<UserAccessArea> listUserAccessAreas(UserAccessAreaAdminFilter filter);

    List<ManagementRelation> listManagementRelations(ManagementRelationAdminFilter filter);

    List<TemporaryRoleAssignment> listTemporaryRoleAssignments(TemporaryRoleAssignmentAdminFilter filter);

    List<TemporaryAccessArea> listTemporaryAccessAreas(TemporaryAccessAreaAdminFilter filter);

    List<TemporaryManagementDelegation> listTemporaryManagementDelegations(TemporaryManagementDelegationAdminFilter filter);

    List<ManagementRelationType> listManagementRelationTypes();
}
