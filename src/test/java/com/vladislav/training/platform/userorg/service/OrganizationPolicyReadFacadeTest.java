package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadScope;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code OrganizationPolicyReadFacade}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationPolicyReadFacadeTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T12:00:00Z");

    @Mock
    private OrganizationPolicyReadService policyReadService;
    @Mock
    private AccessSpecificationPolicy accessSpecificationPolicy;
    @Mock
    private AccessPolicyQueryContextResolver queryContextResolver;
    @Mock
    private OrganizationQueryService organizationQueryService;

    @Test
    void organizationalUnitDetailUsesPolicyScopeBeforeMaterialization() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            20L,
            "organizational_unit"
        );
        AccessReadScope scope = AccessReadScope.scoped(Set.of(20L), Set.of());
        OrganizationalUnit unit = new OrganizationalUnit(20L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.DETAIL, null, 20L, "organizational_unit"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findOrganizationalUnitByIdWithinScope(scope, 20L)).thenReturn(Optional.of(unit));

        assertThat(service.findOrganizationalUnitById(20L)).isEqualTo(unit);
        verify(policyReadService).findOrganizationalUnitByIdWithinScope(scope, 20L);
        verifyNoInteractions(organizationQueryService);
    }

    @Test
    void organizationalUnitPathDetailUsesPolicyScopeBeforeMaterialization() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.DETAIL,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        OrganizationalUnit unit = new OrganizationalUnit(20L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.DETAIL, null, null, "organizational_unit"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findOrganizationalUnitByPathWithinScope(scope, "/root/branch")).thenReturn(Optional.of(unit));

        assertThat(service.findOrganizationalUnitByPath("/root/branch")).isEqualTo(unit);
        verify(policyReadService).findOrganizationalUnitByPathWithinScope(scope, "/root/branch");
    }

    @Test
    void childUnitListUsesPolicyScopeBeforeMaterialization() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            10L,
            "organizational_unit"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        OrganizationalUnit child = new OrganizationalUnit(20L, 10L, 10L, "Child", OrganizationalUnitStatus.ACTIVE, "/root/child", 1, null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.LIST, null, 10L, "organizational_unit"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findChildUnitsWithinScope(scope, 10L)).thenReturn(List.of(child));

        assertThat(service.findChildUnits(10L)).containsExactly(child);
        verify(policyReadService).findChildUnitsWithinScope(scope, 10L);
    }

    @Test
    void organizationalUnitListUsesPolicyScopeBeforeMaterialization() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.LIST,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        OrganizationalUnit unit = new OrganizationalUnit(20L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.LIST, null, null, "organizational_unit"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE)).thenReturn(List.of(unit));

        assertThat(service.findOrganizationalUnits(OrganizationalUnitStatus.ACTIVE)).containsExactly(unit);
        verify(policyReadService).findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE);
    }

    @Test
    void organizationalUnitTreeUsesPolicyScopeBeforeMaterialization() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.TREE,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit"
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        OrganizationalUnit unit = new OrganizationalUnit(20L, 1L, 10L, "Branch", OrganizationalUnitStatus.ACTIVE, "/root/branch", 1, null, FIXED_INSTANT, FIXED_INSTANT);
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.TREE, null, null, "organizational_unit"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(scope);
        when(policyReadService.findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE)).thenReturn(List.of(unit));

        assertThat(service.findOrganizationalUnitTree(OrganizationalUnitStatus.ACTIVE)).containsExactly(unit);
        verify(policyReadService).findUnitsWithinScope(scope, OrganizationalUnitStatus.ACTIVE);
    }

    @Test
    void organizationalUnitTypeReferenceReadRejectsDeniedPolicy() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.REFERENCE_READ,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit_type"
        );
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.REFERENCE_READ, "organizational_unit_type"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.denyAll());

        assertThatThrownBy(service::findAllOrganizationalUnitTypes)
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("forbidden");

        verifyNoInteractions(organizationQueryService, policyReadService);
    }

    @Test
    void organizationalUnitTypeReferenceReadDelegatesToOwnerQueryServiceAfterPolicyCheck() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
            policyReadService,
            accessSpecificationPolicy,
            queryContextResolver,
            organizationQueryService
        );
        AccessPolicyQueryContext context = new AccessPolicyQueryContext(
            101L,
            AccessReadArea.ORGANIZATION,
            AccessReadType.REFERENCE_READ,
            FIXED_INSTANT,
            null,
            null,
            "organizational_unit_type"
        );
        OrganizationalUnitType type = new OrganizationalUnitType(
            10L,
            "DIVISION",
            "Division",
            null,
            OrganizationalNodeKind.LINEAR,
            true,
            true,
            true,
            true,
            true,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
        when(queryContextResolver.resolve(AccessReadArea.ORGANIZATION, AccessReadType.REFERENCE_READ, "organizational_unit_type"))
            .thenReturn(context);
        when(accessSpecificationPolicy.resolveReadScope(context)).thenReturn(AccessReadScope.fullAccess());
        when(organizationQueryService.findUnitTypesByNodeKind(OrganizationalNodeKind.LINEAR)).thenReturn(List.of(type));

        assertThat(service.findAllOrganizationalUnitTypes()).containsExactly(type);
        verify(organizationQueryService).findUnitTypesByNodeKind(OrganizationalNodeKind.LINEAR);
    }
    @Test
    void batchOrganizationalUnitLookupUsesExistingPolicyScopeWithoutNewResolverRoundtrip() {
        OrganizationPolicyReadFacade service = new OrganizationPolicyReadFacade(
                policyReadService,
                accessSpecificationPolicy,
                queryContextResolver,
                organizationQueryService
        );
        AccessReadScope scope = AccessReadScope.fullAccess();
        OrganizationalUnit first = new OrganizationalUnit(
                20L, 1L, 10L, "Branch A", OrganizationalUnitStatus.ACTIVE, "/root/a", 1, null, FIXED_INSTANT, FIXED_INSTANT
        );
        OrganizationalUnit second = new OrganizationalUnit(
                30L, 1L, 10L, "Branch B", OrganizationalUnitStatus.ACTIVE, "/root/b", 1, null, FIXED_INSTANT, FIXED_INSTANT
        );
        when(policyReadService.findOrganizationalUnitsByIdsWithinScope(scope, List.of(20L, 30L)))
                .thenReturn(List.of(first, second));

        assertThat(service.findOrganizationalUnitsByIdsWithinScope(scope, List.of(20L, 30L)))
                .containsEntry(20L, first)
                .containsEntry(30L, second);

        verify(policyReadService).findOrganizationalUnitsByIdsWithinScope(scope, List.of(20L, 30L));
        verifyNoInteractions(accessSpecificationPolicy, queryContextResolver, organizationQueryService);
    }
}
