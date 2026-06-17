package com.vladislav.training.platform.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.access.domain.TemporaryManagementDelegation;
import com.vladislav.training.platform.access.repository.TemporaryManagementDelegationRepository;
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
 * Проверяет поведение {@code TemporaryManagementDelegationCommandServiceImpl}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
@ExtendWith(MockitoExtension.class)
class TemporaryManagementDelegationCommandServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-29T10:00:00Z");

    @Mock
    private TemporaryManagementDelegationRepository temporaryManagementDelegationRepository;
    @Mock
    private AccessCommandValidationSupport accessCommandValidationSupport;
    @Mock
    private UtcClock utcClock;

    private TemporaryManagementDelegationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TemporaryManagementDelegationCommandServiceImpl(
            temporaryManagementDelegationRepository,
            accessCommandValidationSupport,
            utcClock
        );
        lenient().when(utcClock.now()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void saveTemporaryManagementDelegationValidatesThenPersistsCanonicalRow() {
        when(temporaryManagementDelegationRepository.saveTemporaryManagementDelegation(any(TemporaryManagementDelegation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TemporaryManagementDelegation saved = service.saveTemporaryManagementDelegation(delegation(
            null,
            1L,
            30L,
            500L,
            FIXED_INSTANT.minusSeconds(7200),
            null
        ));

        ArgumentCaptor<TemporaryManagementDelegation> captor = ArgumentCaptor.forClass(TemporaryManagementDelegation.class);
        InOrder inOrder = inOrder(accessCommandValidationSupport, temporaryManagementDelegationRepository);
        inOrder.verify(accessCommandValidationSupport).ensureTemporaryManagementAssignable(any(TemporaryManagementDelegation.class));
        inOrder.verify(temporaryManagementDelegationRepository).saveTemporaryManagementDelegation(captor.capture());

        TemporaryManagementDelegation persisted = captor.getValue();
        assertThat(persisted.id()).isNull();
        assertThat(persisted.userId()).isEqualTo(1L);
        assertThat(persisted.organizationalUnitId()).isEqualTo(30L);
        assertThat(persisted.managementRelationTypeId()).isEqualTo(500L);
        assertThat(persisted.validTo()).isNull();
        assertThat(saved.createdAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void saveTemporaryManagementDelegationRejectsPreExistingIdBeforeOwnerValidation() {
        assertThatThrownBy(() -> service.saveTemporaryManagementDelegation(delegation(
            99L,
            1L,
            30L,
            500L,
            FIXED_INSTANT,
            null
        )))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("temporaryManagementDelegation.id must be null");

        verifyNoInteractions(accessCommandValidationSupport, temporaryManagementDelegationRepository);
    }

    @Test
    void closeActiveTemporaryManagementDelegationsByUserIdClosesAllRowsWithSingleEffectiveAt() {
        TemporaryManagementDelegation first = delegation(10L, 1L, 30L, 500L, FIXED_INSTANT.minusSeconds(7200), null);
        TemporaryManagementDelegation second = delegation(11L, 1L, 31L, 501L, FIXED_INSTANT.minusSeconds(3600), null);
        when(temporaryManagementDelegationRepository.findActiveTemporaryManagementDelegationsByUserId(1L, FIXED_INSTANT))
            .thenReturn(List.of(first, second));
        when(temporaryManagementDelegationRepository.findTemporaryManagementDelegationById(10L))
            .thenReturn(delegation(10L, 1L, 30L, 500L, first.validFrom(), FIXED_INSTANT));
        when(temporaryManagementDelegationRepository.findTemporaryManagementDelegationById(11L))
            .thenReturn(delegation(11L, 1L, 31L, 501L, second.validFrom(), FIXED_INSTANT));

        List<TemporaryManagementDelegation> closedDelegations = service.closeActiveTemporaryManagementDelegationsByUserId(
            1L,
            FIXED_INSTANT
        );

        assertThat(closedDelegations).hasSize(2);
        assertThat(closedDelegations).allSatisfy(delegation -> assertThat(delegation.validTo()).isEqualTo(FIXED_INSTANT));
        verify(accessCommandValidationSupport).ensureTemporaryManagementClosable(10L, FIXED_INSTANT);
        verify(accessCommandValidationSupport).ensureTemporaryManagementClosable(11L, FIXED_INSTANT);
        verify(temporaryManagementDelegationRepository).endTemporaryManagementDelegation(10L, FIXED_INSTANT);
        verify(temporaryManagementDelegationRepository).endTemporaryManagementDelegation(11L, FIXED_INSTANT);
    }

    private TemporaryManagementDelegation delegation(
        Long id,
        Long userId,
        Long organizationalUnitId,
        Long typeId,
        Instant validFrom,
        Instant validTo
    ) {
        return new TemporaryManagementDelegation(id, userId, organizationalUnitId, typeId, validFrom, validTo, FIXED_INSTANT, FIXED_INSTANT);
    }
}
