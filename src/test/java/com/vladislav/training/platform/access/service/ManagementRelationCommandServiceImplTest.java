package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.ManagementRelation;
import com.vladislav.training.platform.access.repository.ManagementRelationRepository;
import com.vladislav.training.platform.common.exception.ValidationException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение {@code ManagementRelationCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class ManagementRelationCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private ManagementRelationRepository managementRelationRepository;
    @Mock
    private AccessCommandValidationSupport accessCommandValidationSupport;
    @Mock
    private UtcClock utcClock;

    private ManagementRelationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ManagementRelationCommandServiceImpl(managementRelationRepository, accessCommandValidationSupport, utcClock);
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void saveManagementRelationValidatesThenPersistsCanonicalRow() {
        when(managementRelationRepository.saveManagementRelation(any(ManagementRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManagementRelation saved = service.saveManagementRelation(relation(
            null,
            1L,
            30L,
            500L,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        ArgumentCaptor<ManagementRelation> captor = ArgumentCaptor.forClass(ManagementRelation.class);
        InOrder inOrder = inOrder(accessCommandValidationSupport, managementRelationRepository);
        inOrder.verify(accessCommandValidationSupport).ensureManagementRelationAssignable(any(ManagementRelation.class));
        inOrder.verify(managementRelationRepository).saveManagementRelation(captor.capture());

        ManagementRelation persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(1L);
        assertThat(persisted.organizationalUnitId()).isEqualTo(30L);
        assertThat(persisted.managementRelationTypeId()).isEqualTo(500L);
        assertThat(persisted.validTo()).isNull();
        assertThat(persisted.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(saved.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void saveManagementRelationRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.saveManagementRelation(relation(
            99L,
            1L,
            30L,
            500L,
            FIXED_INSTANT,
            null
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("managementRelation.id must be null");

        verifyNoInteractions(accessCommandValidationSupport, managementRelationRepository);
    }

    @Test
    void closeActiveManagementRelationsByUserIdClosesAllRowsWithSingleEffectiveAt() {
        ManagementRelation first = relation(10L, 1L, 30L, 500L, FIXED_INSTANT.minusSeconds(7200), null);
        ManagementRelation second = relation(11L, 1L, 31L, 501L, FIXED_INSTANT.minusSeconds(3600), null);
        when(managementRelationRepository.findActiveManagementRelationsByUserId(1L, FIXED_INSTANT)).thenReturn(List.of(first, second));
        when(managementRelationRepository.findManagementRelationById(10L)).thenReturn(relation(10L, 1L, 30L, 500L, first.validFrom(), FIXED_INSTANT));
        when(managementRelationRepository.findManagementRelationById(11L)).thenReturn(relation(11L, 1L, 31L, 501L, second.validFrom(), FIXED_INSTANT));

        List<ManagementRelation> closedRelations = service.closeActiveManagementRelationsByUserId(1L, FIXED_INSTANT);

        assertThat(closedRelations).hasSize(2);
        assertThat(closedRelations).allSatisfy(relation -> assertThat(relation.validTo()).isEqualTo(FIXED_INSTANT));
        verify(accessCommandValidationSupport).ensureManagementRelationClosable(10L, FIXED_INSTANT);
        verify(accessCommandValidationSupport).ensureManagementRelationClosable(11L, FIXED_INSTANT);
        verify(managementRelationRepository).endManagementRelation(10L, FIXED_INSTANT);
        verify(managementRelationRepository).endManagementRelation(11L, FIXED_INSTANT);
    }

    private ManagementRelation relation(
        Long id,
        Long userId,
        Long organizationalUnitId,
        Long typeId,
        Instant validFrom,
        Instant validTo
    ) {
        return new ManagementRelation(id, userId, organizationalUnitId, typeId, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }
}
