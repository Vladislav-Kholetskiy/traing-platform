package com.vladislav.training.platform.access.service;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code AccessAdministrationQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class AccessAdministrationQueryServiceImpl implements AccessAdministrationQueryService {

    private final AccessAdministrationPolicyReadService policyReadService;
    private final ManagementRelationTypeQueryService managementRelationTypeQueryService;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver queryContextResolver;

    public AccessAdministrationQueryServiceImpl(
        AccessAdministrationPolicyReadService policyReadService,
        ManagementRelationTypeQueryService managementRelationTypeQueryService,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver queryContextResolver
    ) {
        this.policyReadService = policyReadService;
        this.managementRelationTypeQueryService = managementRelationTypeQueryService;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.queryContextResolver = queryContextResolver;
    }

    @Override
    public List<UserAccessArea> listUserAccessAreas(UserAccessAreaAdminFilter filter) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            filter.userId(),
            filter.organizationalUnitId(),
            "user_access_area"
        );
        AccessReadScope scope = ensureReadAllowed(context);
        return policyReadService.findUserAccessAreasWithinScope(scope, context.effectiveAt(), filter);
    }

    @Override
    public List<ManagementRelation> listManagementRelations(ManagementRelationAdminFilter filter) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            filter.userId(),
            filter.organizationalUnitId(),
            "management_relation"
        );
        AccessReadScope scope = ensureReadAllowed(context);
        return policyReadService.findManagementRelationsWithinScope(scope, filter);
    }

    @Override
    public List<TemporaryRoleAssignment> listTemporaryRoleAssignments(TemporaryRoleAssignmentAdminFilter filter) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
            AccessReadArea.TEMPORARY_AUTHORITY,
            AccessReadType.HISTORY,
            filter.userId(),
            null,
            "temporary_role_assignment"
        );
        AccessReadScope scope = ensureReadAllowed(context);
        return policyReadService.findTemporaryRoleAssignmentsWithinScope(scope, context.effectiveAt(), filter);
    }

    @Override
    public List<TemporaryAccessArea> listTemporaryAccessAreas(TemporaryAccessAreaAdminFilter filter) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
            AccessReadArea.TEMPORARY_AUTHORITY,
            AccessReadType.HISTORY,
            filter.userId(),
            filter.organizationalUnitId(),
            "temporary_access_area"
        );
        AccessReadScope scope = ensureReadAllowed(context);
        return policyReadService.findTemporaryAccessAreasWithinScope(scope, context.effectiveAt(), filter);
    }

    @Override
    public List<TemporaryManagementDelegation> listTemporaryManagementDelegations(
        TemporaryManagementDelegationAdminFilter filter
    ) {
        AccessPolicyQueryContext context = queryContextResolver.resolve(
            AccessReadArea.TEMPORARY_AUTHORITY,
            AccessReadType.HISTORY,
            filter.userId(),
            filter.organizationalUnitId(),
            "temporary_management_delegation"
        );
        AccessReadScope scope = ensureReadAllowed(context);
        return policyReadService.findTemporaryManagementDelegationsWithinScope(scope, filter);
    }

    @Override
    public List<ManagementRelationType> listManagementRelationTypes() {
        ensureReadAllowed(queryContextResolver.resolve(
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.REFERENCE_READ,
            "management_relation_type"
        ));
        return managementRelationTypeQueryService.findAllManagementRelationTypes();
    }

    private AccessReadScope ensureReadAllowed(AccessPolicyQueryContext context) {
        AccessReadScope scope = accessSpecificationPolicy.resolveReadScope(context);
        if (!scope.readAllowed()) {
            throw new PolicyViolationException("Access & Management read is forbidden by AccessSpecificationPolicy");
        }
        return scope;
    }
}
