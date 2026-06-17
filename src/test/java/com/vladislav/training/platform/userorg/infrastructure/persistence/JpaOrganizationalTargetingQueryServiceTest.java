package com.vladislav.training.platform.userorg.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.userorg.service.OrganizationalTargetingQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code JpaOrganizationalTargetingQuery}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class JpaOrganizationalTargetingQueryServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-09T11:00:00Z");

    @Mock
    private SpringDataOrganizationalUnitJpaRepository organizationalUnitRepository;
    @Mock
    private SpringDataUserOrganizationAssignmentJpaRepository userOrganizationAssignmentRepository;

    @Test
    void resolvesCurrentCandidateUserIdsFromOrganizationalSubtree() {
        OrganizationalTargetingQueryService service = new JpaOrganizationalTargetingQueryService(
            organizationalUnitRepository,
            userOrganizationAssignmentRepository
        );

        when(organizationalUnitRepository.findAllInSubtreeByPath("/company/ops")).thenReturn(List.of(
            organizationalUnit(41L, "/company/ops"),
            organizationalUnit(42L, "/company/ops/team-a")
        ));
        when(userOrganizationAssignmentRepository.findDistinctActiveUserIdsByOrganizationalUnitIdIn(
            List.of(41L, 42L),
            FIXED_INSTANT
        )).thenReturn(List.of(201L, 202L, 201L));

        assertThat(service.resolveCurrentCandidateUserIdsForUnitSubtree("/company/ops", FIXED_INSTANT))
            .containsExactlyInAnyOrder(201L, 202L);

        verify(organizationalUnitRepository).findAllInSubtreeByPath("/company/ops");
        verify(userOrganizationAssignmentRepository).findDistinctActiveUserIdsByOrganizationalUnitIdIn(
            List.of(41L, 42L),
            FIXED_INSTANT
        );
    }

    @Test
    void returnsEmptySetWhenOrganizationalSubtreeHasNoUnits() {
        OrganizationalTargetingQueryService service = new JpaOrganizationalTargetingQueryService(
            organizationalUnitRepository,
            userOrganizationAssignmentRepository
        );

        when(organizationalUnitRepository.findAllInSubtreeByPath("/company/missing")).thenReturn(List.of());

        assertThat(service.resolveCurrentCandidateUserIdsForUnitSubtree("/company/missing", FIXED_INSTANT)).isEmpty();

        verify(organizationalUnitRepository).findAllInSubtreeByPath("/company/missing");
        verifyNoInteractions(userOrganizationAssignmentRepository);
    }

    private OrganizationalUnitEntity organizationalUnit(Long id, String path) {
        OrganizationalUnitEntity entity = new OrganizationalUnitEntity();
        entity.setId(id);
        entity.setPath(path);
        return entity;
    }
}
