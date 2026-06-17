package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.userorg.domain.OrganizationalNodeKind;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitType;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitRepository;
import com.vladislav.training.platform.userorg.repository.OrganizationalUnitTypeRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code OrganizationQueryServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationQueryServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock
    private OrganizationalUnitTypeRepository organizationalUnitTypeRepository;

    private OrganizationQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrganizationQueryServiceImpl(organizationalUnitRepository, organizationalUnitTypeRepository);
    }

    @Test
    void findChildUnitsReturnsRepositoryOrderedSnapshot() {
        List<OrganizationalUnit> children = List.of(
            new OrganizationalUnit(2L, 1L, 10L, "A", OrganizationalUnitStatus.ACTIVE, "/root/a", 1, null, FIXED_INSTANT, FIXED_INSTANT),
            new OrganizationalUnit(3L, 1L, 10L, "B", OrganizationalUnitStatus.ACTIVE, "/root/b", 1, null, FIXED_INSTANT, FIXED_INSTANT)
        );
        when(organizationalUnitRepository.findChildUnits(1L)).thenReturn(children);

        assertThat(service.findChildUnits(1L)).containsExactlyElementsOf(children);
        verify(organizationalUnitRepository).findChildUnits(1L);
    }

    @Test
    void findUnitTypesByNodeKindDelegatesToRepository() {
        List<OrganizationalUnitType> unitTypes = List.of(
            new OrganizationalUnitType(10L, "DIVISION", "Division", null, OrganizationalNodeKind.LINEAR, true, true, true, true, true, FIXED_INSTANT, FIXED_INSTANT)
        );
        when(organizationalUnitTypeRepository.findUnitTypesByNodeKind(OrganizationalNodeKind.LINEAR)).thenReturn(unitTypes);

        assertThat(service.findUnitTypesByNodeKind(OrganizationalNodeKind.LINEAR)).containsExactlyElementsOf(unitTypes);
        verify(organizationalUnitTypeRepository).findUnitTypesByNodeKind(OrganizationalNodeKind.LINEAR);
    }
}