package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.AccessScopeType;
import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.ManagementRelationType;
import com.vladislav.training.platform.access.domain.TemporaryAccessArea;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.domain.TemporaryRoleAssignment;
import com.vladislav.training.platform.access.domain.UserAccessArea;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AccessAdministrationQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AccessAdministrationQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T12:00:00Z");

    @Mock
    private AccessAdministrationPolicyReadService policyReadService;
    @Mock
    private ManagementRelationTypeQueryService managementRelationTypeQueryService;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;

    @Test
    void userAccessAreaHistoryUsesPolicyReadPort() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        UserAccessAreaAdminFilter filter = new UserAccessAreaAdminFilter(55L, 30L, AccessScopeType.UNIT_ONLY, FIXED_INSTANT);
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            55L,
            30L,
            "user_access_area"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        UserAccessArea area = new UserAccessArea(1L, 55L, 30L, AccessScopeType.UNIT_ONLY, FIXED_INSTANT.minusSeconds(3600), null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ACCESS_MANAGEMENT, AccessReadType.HISTORY, 55L, 30L, "user_access_area"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findUserAccessAreasWithinScope(scope, FIXED_INSTANT, filter)).thenReturn(List.of(area));

        assertThat(service.listUserAccessAreas(filter)).containsExactly(area);
        verify(policyReadService).findUserAccessAreasWithinScope(scope, FIXED_INSTANT, filter);
    }

    @Test
    void managementRelationHistoryUsesPolicyReadPort() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        ManagementRelationAdminFilter filter = new ManagementRelationAdminFilter(55L, 30L, 500L, FIXED_INSTANT);
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            55L,
            30L,
            "management_relation"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        ManagementRelation relation = new ManagementRelation(1L, 55L, 30L, 500L, FIXED_INSTANT.minusSeconds(3600), null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ACCESS_MANAGEMENT, AccessReadType.HISTORY, 55L, 30L, "management_relation"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findManagementRelationsWithinScope(scope, filter)).thenReturn(List.of(relation));

        assertThat(service.listManagementRelations(filter)).containsExactly(relation);
        verify(policyReadService).findManagementRelationsWithinScope(scope, filter);
    }

    @Test
    void temporaryRoleHistoryUsesPolicyReadPort() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        TemporaryRoleAssignmentAdminFilter filter = new TemporaryRoleAssignmentAdminFilter(55L, 900L, FIXED_INSTANT);
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.TEMPORARY_AUTHORITY,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            55L,
            null,
            "temporary_role_assignment"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        TemporaryRoleAssignment assignment = new TemporaryRoleAssignment(1L, 55L, 900L, FIXED_INSTANT.minusSeconds(3600), null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.TEMPORARY_AUTHORITY, AccessReadType.HISTORY, 55L, null, "temporary_role_assignment"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findTemporaryRoleAssignmentsWithinScope(scope, FIXED_INSTANT, filter))
            .thenReturn(List.of(assignment));

        assertThat(service.listTemporaryRoleAssignments(filter)).containsExactly(assignment);
        verify(policyReadService).findTemporaryRoleAssignmentsWithinScope(scope, FIXED_INSTANT, filter);
    }

    @Test
    void temporaryAccessAreaHistoryUsesPolicyReadPort() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        TemporaryAccessAreaAdminFilter filter = new TemporaryAccessAreaAdminFilter(55L, 30L, AccessScopeType.UNIT_SUBTREE, FIXED_INSTANT);
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.TEMPORARY_AUTHORITY,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            55L,
            30L,
            "temporary_access_area"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        TemporaryAccessArea area = new TemporaryAccessArea(1L, 55L, 30L, AccessScopeType.UNIT_SUBTREE, FIXED_INSTANT.minusSeconds(3600), null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.TEMPORARY_AUTHORITY, AccessReadType.HISTORY, 55L, 30L, "temporary_access_area"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findTemporaryAccessAreasWithinScope(scope, FIXED_INSTANT, filter)).thenReturn(List.of(area));

        assertThat(service.listTemporaryAccessAreas(filter)).containsExactly(area);
        verify(policyReadService).findTemporaryAccessAreasWithinScope(scope, FIXED_INSTANT, filter);
    }

    @Test
    void temporaryManagementDelegationHistoryUsesPolicyReadPort() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        TemporaryManagementDelegationAdminFilter filter = new TemporaryManagementDelegationAdminFilter(55L, 30L, 500L, FIXED_INSTANT);
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.TEMPORARY_AUTHORITY,
            AccessReadType.HISTORY,
            FIXED_INSTANT,
            55L,
            30L,
            "temporary_management_delegation"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        TemporaryManagementDelegation delegation = new TemporaryManagementDelegation(1L, 55L, 30L, 500L, FIXED_INSTANT.minusSeconds(3600), null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.TEMPORARY_AUTHORITY, AccessReadType.HISTORY, 55L, 30L, "temporary_management_delegation"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findTemporaryManagementDelegationsWithinScope(scope, filter)).thenReturn(List.of(delegation));

        assertThat(service.listTemporaryManagementDelegations(filter)).containsExactly(delegation);
        verify(policyReadService).findTemporaryManagementDelegationsWithinScope(scope, filter);
    }

    @Test
    void relationTypeReferenceReadRejectsDeniedPolicyBeforeRepositoryMaterialization() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.REFERENCE_READ,
            FIXED_INSTANT,
            null,
            null,
            "management_relation_type"
        );
        when(queryContextResolver.resolve(AccessReadArea.ACCESS_MANAGEMENT, AccessReadType.REFERENCE_READ, "management_relation_type"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.denyAll());

        assertThatThrownBy(service::listManagementRelationTypes)
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("Access & Management read is forbidden");

        verifyNoInteractions(managementRelationTypeQueryService, policyReadService);
    }

    @Test
    void relationTypeReferenceReadAllowsPolicyAwareDictionaryLoad() {
        AccessAdministrationQueryServiceImpl service = new AccessAdministrationQueryServiceImpl(
            policyReadService,
            managementRelationTypeQueryService,
            accessSpecificationPolicy,
            queryContextResolver
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ACCESS_MANAGEMENT,
            AccessReadType.REFERENCE_READ,
            FIXED_INSTANT,
            null,
            null,
            "management_relation_type"
        );
        ManagementRelationType relationType = new ManagementRelationType(5L, "SUPERVISOR", "Supervisor", null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ACCESS_MANAGEMENT, AccessReadType.REFERENCE_READ, "management_relation_type"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.fullAccess());
        when(managementRelationTypeQueryService.findAllManagementRelationTypes()).thenReturn(List.of(relationType));

        assertThat(service.listManagementRelationTypes()).containsExactly(relationType);
        verify(managementRelationTypeQueryService).findAllManagementRelationTypes();
    }
}
