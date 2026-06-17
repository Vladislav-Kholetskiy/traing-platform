package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.access.repository.ManagementRelationTypeRepository;
import com.vladislav.training.platform.access.repository.TemporaryAccessAreaRepository;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
import com.vladislav.training.platform.access.repository.TemporaryRoleAssignmentRepository;
import com.vladislav.training.platform.access.repository.UserAccessAreaRepository;
import com.vladislav.training.platform.userorg.service.UserOrgFoundationStateReadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code AccessFoundationStateReadServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class AccessFoundationStateReadServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-04T17:00:00Z");

    @Mock
    private UserAccessAreaRepository userAccessAreaRepository;
    @Mock
    private ManagementRelationRepository managementRelationRepository;
    @Mock
    private TemporaryAccessAreaRepository temporaryAccessAreaRepository;
    @Mock
    private TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;
    @Mock
    private TemporaryRoleAssignmentRepository temporaryRoleAssignmentRepository;
    @Mock
    private ManagementRelationTypeRepository managementRelationTypeRepository;
    @Mock
    private UserOrgFoundationStateReadService userOrgFoundationStateReadService;

    @Test
    void activeManagementRelationGivesUnitAnchorWithoutAutomaticSubtreeScope() {
        AccessFoundationStateReadServiceImpl service = service();
        when(managementRelationRepository.findActiveManagementRelationsByUserId(101L, FIXED_INSTANT)).thenReturn(
            List.of(managementRelation(1L, 101L, 30L))
        );
        when(temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of());
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(30L)).thenReturn(
            organizationalUnit(30L, "/dept/30", false)
        );

        AccessFoundationStateReadService.ManagerialScopeFoundationState scope = service.findActorManagerialScope(
            101L,
            FIXED_INSTANT
        );

        assertThat(scope.unitAnchorIds()).containsExactly(30L);
        assertThat(scope.subtreePaths()).isEmpty();
        verify(managementRelationRepository).findActiveManagementRelationsByUserId(101L, FIXED_INSTANT);
        verify(temporaryManagementDelegationRepository).findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT);
        verify(userOrgFoundationStateReadService).findOrganizationalUnitFoundationState(30L);
        verifyNoMoreInteractions(
            managementRelationRepository,
            temporaryManagementDelegationRepository,
            userOrgFoundationStateReadService
        );
    }

    @Test
    void inactiveOrExpiredRelationDoesNotProduceAnchorWhenRepositoryReturnsNoActiveRows() {
        AccessFoundationStateReadServiceImpl service = service();
        when(managementRelationRepository.findActiveManagementRelationsByUserId(101L, FIXED_INSTANT)).thenReturn(List.of());
        when(temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of());

        AccessFoundationStateReadService.ManagerialScopeFoundationState scope = service.findActorManagerialScope(
            101L,
            FIXED_INSTANT
        );

        assertThat(scope.unitAnchorIds()).isEmpty();
        assertThat(scope.subtreePaths()).isEmpty();
        verify(managementRelationRepository).findActiveManagementRelationsByUserId(101L, FIXED_INSTANT);
        verify(temporaryManagementDelegationRepository).findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT);
        verify(userOrgFoundationStateReadService, never()).findOrganizationalUnitFoundationState(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void activeTemporaryDelegationGivesUnitAnchorAndOwnerFactsMayPromoteOnlyConfirmedSubtreePath() {
        AccessFoundationStateReadServiceImpl service = service();
        when(managementRelationRepository.findActiveManagementRelationsByUserId(101L, FIXED_INSTANT)).thenReturn(List.of());
        when(temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of(temporaryDelegation(10L, 101L, 41L)));
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(41L)).thenReturn(
            organizationalUnit(41L, "/dept/41", true)
        );

        AccessFoundationStateReadService.ManagerialScopeFoundationState scope = service.findActorManagerialScope(
            101L,
            FIXED_INSTANT
        );

        assertThat(scope.unitAnchorIds()).containsExactly(41L);
        assertThat(scope.subtreePaths()).containsExactly("/dept/41");
    }

    @Test
    void expiredDelegationDoesNotProduceAnchorWhenRepositoryReturnsNoActiveRows() {
        AccessFoundationStateReadServiceImpl service = service();
        when(managementRelationRepository.findActiveManagementRelationsByUserId(101L, FIXED_INSTANT)).thenReturn(List.of());
        when(temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of());

        AccessFoundationStateReadService.ManagerialScopeFoundationState scope = service.findActorManagerialScope(
            101L,
            FIXED_INSTANT
        );

        assertThat(scope.unitAnchorIds()).isEmpty();
        assertThat(scope.subtreePaths()).isEmpty();
    }

    @Test
    void duplicateAnchorsFromPermanentAndTemporarySourcesCollapseToSingleUnitAndConfirmedSubtreePath() {
        AccessFoundationStateReadServiceImpl service = service();
        when(managementRelationRepository.findActiveManagementRelationsByUserId(101L, FIXED_INSTANT)).thenReturn(
            List.of(managementRelation(1L, 101L, 30L))
        );
        when(temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(101L, FIXED_INSTANT))
            .thenReturn(List.of(temporaryDelegation(10L, 101L, 30L)));
        when(userOrgFoundationStateReadService.findOrganizationalUnitFoundationState(30L)).thenReturn(
            organizationalUnit(30L, "/dept/30", true)
        );

        AccessFoundationStateReadService.ManagerialScopeFoundationState scope = service.findActorManagerialScope(
            101L,
            FIXED_INSTANT
        );

        assertThat(scope.unitAnchorIds()).containsExactly(30L);
        assertThat(scope.subtreePaths()).containsExactly("/dept/30");
        verify(userOrgFoundationStateReadService).findOrganizationalUnitFoundationState(30L);
    }

    @Test
    void accessFoundationManagerialScopeRemainsReadOnlyAndDoesNotDependOnCommandServices() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/access/service/AccessFoundationStateReadServiceImpl.java"
        ));

        assertThat(source)
            .contains("findActiveManagementRelationsByUserId")
            .contains("findActiveTemporaryManagementDelegationsByUserId")
            .contains("findOrganizationalUnitFoundationState")
            .doesNotContain("ManagementRelationCommandService")
            .doesNotContain("TemporaryManagementDelegationCommandService")
            .doesNotContain("AssignmentCommandService")
            .doesNotContain("saveManagementRelation")
            .doesNotContain("endManagementRelation")
            .doesNotContain("saveTemporaryManagementDelegation")
            .doesNotContain("endTemporaryManagementDelegation")
            .doesNotContain(".save(")
            .doesNotContain(".delete(");
    }

    private AccessFoundationStateReadServiceImpl service() {
        return new AccessFoundationStateReadServiceImpl(
            userAccessAreaRepository,
            managementRelationRepository,
            temporaryAccessAreaRepository,
            temporaryManagementDelegationRepository,
            temporaryRoleAssignmentRepository,
            managementRelationTypeRepository,
            userOrgFoundationStateReadService
        );
    }

    private ManagementRelation managementRelation(Long id, Long userId, Long unitId) {
        return new ManagementRelation(
            id,
            userId,
            unitId,
            500L,
            FIXED_INSTANT.minusSeconds(7200),
            null,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(3600)
        );
    }

    private TemporaryManagementDelegation temporaryDelegation(Long id, Long userId, Long unitId) {
        return new TemporaryManagementDelegation(
            id,
            userId,
            unitId,
            500L,
            FIXED_INSTANT.minusSeconds(7200),
            null,
            FIXED_INSTANT.minusSeconds(7200),
            FIXED_INSTANT.minusSeconds(3600)
        );
    }

    private UserOrgFoundationStateReadService.OrganizationalUnitFoundationState organizationalUnit(
        Long unitId,
        String path,
        boolean participatesInSubtreeScope
    ) {
        return new UserOrgFoundationStateReadService.OrganizationalUnitFoundationState(
            unitId,
            true,
            path,
            false,
            true,
            false,
            true,
            true,
            participatesInSubtreeScope
        );
    }
}
